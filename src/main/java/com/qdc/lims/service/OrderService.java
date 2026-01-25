package com.qdc.lims.service;

import com.qdc.lims.dto.OrderRequest;
import com.qdc.lims.entity.*;
import com.qdc.lims.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for handling lab order creation and related business logic.
 */
@Service
public class OrderService {

    private final LabOrderRepository orderRepo;
    private final PatientRepository patientRepo;
    private final TestDefinitionRepository testRepo;
    private final DoctorRepository doctorRepo;
    private final CommissionLedgerRepository commissionRepo;
    private final TestConsumptionRepository consumptionRepo;
    private final InventoryItemRepository inventoryRepo;
    private final PanelRepository panelRepo;

    /**
     * Constructs an OrderService with all required repositories.
     *
     * @param orderRepo       LabOrder repository
     * @param patientRepo     Patient repository
     * @param testRepo        TestDefinition repository
     * @param doctorRepo      Doctor repository
     * @param commissionRepo  CommissionLedger repository
     * @param consumptionRepo TestConsumption repository
     * @param inventoryRepo   InventoryItem repository
     * @param panelRepo       Panel repository
     */
    public OrderService(LabOrderRepository orderRepo, PatientRepository patientRepo,
            TestDefinitionRepository testRepo, DoctorRepository doctorRepo,
            CommissionLedgerRepository commissionRepo, TestConsumptionRepository consumptionRepo,
            InventoryItemRepository inventoryRepo, PanelRepository panelRepo) {
        this.orderRepo = orderRepo;
        this.patientRepo = patientRepo;
        this.testRepo = testRepo;
        this.doctorRepo = doctorRepo;
        this.commissionRepo = commissionRepo;
        this.consumptionRepo = consumptionRepo;
        this.inventoryRepo = inventoryRepo;
        this.panelRepo = panelRepo;
    }

    /**
     * Creates a new lab order, handles inventory deduction, finance logic, and
     * commission calculation.
     *
     * @param request the order request data
     * @return the saved LabOrder entity
     */
    @Transactional
    public LabOrder createOrder(OrderRequest request) {
        // 1. Find Patient
        Patient patient = patientRepo.findById(request.patientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // 2. Find Doctor (Handle Null for Self-Patients)
        Doctor doctor = null;
        if (request.doctorId() != null) {
            doctor = doctorRepo.findById(request.doctorId()).orElse(null);
        }

        // 3. Setup Order
        LabOrder order = new LabOrder();
        order.setPatient(patient);
        order.setReferringDoctor(doctor);

        java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;

        // --- NEW LOGIC: Expand panels to tests ---
        List<Long> testIds = request.testIds() != null ? request.testIds() : List.of();
        List<Integer> panelIds = request.panelIds() != null ? request.panelIds() : List.of();

        // Fetch tests from panels
        List<TestDefinition> panelTests = List.of();
        if (!panelIds.isEmpty()) {
            List<Panel> panels = panelRepo.findAllById(panelIds);
            panelTests = panels.stream()
                    .flatMap(panel -> panel.getTests().stream())
                    .toList();
        }

        // Fetch individual tests
        List<TestDefinition> individualTests = testRepo.findAllById(testIds);

        // Merge and deduplicate
        List<TestDefinition> allTests = java.util.stream.Stream.concat(individualTests.stream(), panelTests.stream())
                .distinct()
                .toList();

        if (allTests.isEmpty()) {
            throw new RuntimeException("At least one test must be selected to create an order.");
        }

        for (TestDefinition test : allTests) {
            // A. Create Empty Result Slot
            LabResult result = new LabResult();
            result.setLabOrder(order);
            result.setTestDefinition(test);
            result.setResultValue(""); // Waiting for Lab Tech
            result.setStatus("PENDING");
            order.getResults().add(result);

            // B. Add Price to Bill
            if (test.getPrice() != null) {
                totalAmount = totalAmount.add(test.getPrice());
            }

            // C. INVENTORY LOGIC (Automatic Deduction)
            List<TestConsumption> recipe = consumptionRepo.findByTest(test);
            for (TestConsumption ingredient : recipe) {
                InventoryItem item = ingredient.getItem();

                double needed = ingredient.getQuantity();
                double available = item.getCurrentStock();

                // --- THE GUARD CHECK ---
                if (available < needed) {
                    throw new RuntimeException(
                            "❌ OUT OF STOCK: Test '" + test.getTestName() + "' requires "
                                    + needed + " " + item.getUnit() + " of '" + item.getItemName() + "', "
                                    + "but only " + available + " is available.");
                }
                // -----------------------

                // Subtract Stock
                double newStock = item.getCurrentStock() - ingredient.getQuantity();
                if (newStock < 0) {
                    throw new RuntimeException(
                            "❌ OUT OF STOCK: Not enough " + item.getItemName() + " to book this test.");
                }

                item.setCurrentStock(newStock);

                // Save updated stock
                inventoryRepo.save(item);
            }
        }

        // --- NEW FINANCE LOGIC ---
        order.setTotalAmount(totalAmount.doubleValue());
        order.setDiscountAmount(request.discount() != null ? request.discount() : 0.0);
        order.setPaidAmount(request.cashPaid() != null ? request.cashPaid() : 0.0);

        // Auto-calculate balance (Total - Discount - Paid)
        order.calculateBalance();
        // -------------------------
        LabOrder savedOrder = orderRepo.save(order);

        // 4. COMMISSION LOGIC (Secret Table)
        if (doctor != null && doctor.getCommissionPercentage() > 0) {
            CommissionLedger ledger = new CommissionLedger();
            ledger.setLabOrder(savedOrder);
            ledger.setDoctor(doctor);
            ledger.setTotalBillAmount(totalAmount.doubleValue());
            commissionRepo.save(ledger);
        }

        return savedOrder;
    }
}
