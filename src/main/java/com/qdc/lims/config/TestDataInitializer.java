package com.qdc.lims.config;

import com.qdc.lims.entity.*;
import com.qdc.lims.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Initializes financial test data for the LIMS application.
 * Runs after DataSeeder (which seeds Patients, Doctors, Tests).
 */
@Component
@Order(2) // Run after DataSeeder
public class TestDataInitializer implements CommandLineRunner {

    private final LabOrderRepository orderRepo;
    private final PaymentRepository paymentRepo;
    private final CommissionLedgerRepository commissionRepo;
    private final SupplierLedgerRepository supplierLedgerRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;
    private final SupplierRepository supplierRepo;

    public TestDataInitializer(LabOrderRepository orderRepo, PaymentRepository paymentRepo,
            CommissionLedgerRepository commissionRepo, SupplierLedgerRepository supplierLedgerRepo,
            DoctorRepository doctorRepo, PatientRepository patientRepo,
            SupplierRepository supplierRepo) {
        this.orderRepo = orderRepo;
        this.paymentRepo = paymentRepo;
        this.commissionRepo = commissionRepo;
        this.supplierLedgerRepo = supplierLedgerRepo;
        this.doctorRepo = doctorRepo;
        this.patientRepo = patientRepo;
        this.supplierRepo = supplierRepo;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (paymentRepo.count() > 0 || orderRepo.count() > 0) {
            System.out.println("✅ Financial data already exists. Skipping Financial Seeder.");
            return;
        }

        System.out.println("⚡ Seeding Financial Test Data...");

        // Ensure base data exists (from DataSeeder)
        List<Doctor> doctors = doctorRepo.findAll();
        List<Patient> patients = patientRepo.findAll();

        if (doctors.isEmpty() || patients.isEmpty()) {
            System.out.println("⚠️ No doctors or patients found. Skipping financial seeding.");
            return;
        }

        Random random = new Random();

        // 1. Create Suppliers (if not exists)
        if (supplierRepo.count() == 0) {
            createSupplier("MedTech Solutions", "0300-1112222");
            createSupplier("Global Lab Supplies", "0321-3334444");
        }
        List<Supplier> suppliers = supplierRepo.findAll();

        // 2. Generate Past Lab Orders (Income)
        // Orders from last 30 days
        for (int i = 0; i < 15; i++) {
            Patient p = patients.get(random.nextInt(patients.size()));
            Doctor d = doctors.get(random.nextInt(doctors.size()));

            LabOrder order = new LabOrder();
            order.setPatient(p);
            order.setReferringDoctor(d);

            LocalDateTime date = LocalDateTime.now().minusDays(random.nextInt(30));
            order.setOrderDate(date);
            order.setDeliveryDate(date.plusDays(1));
            order.setReportDelivered(true);

            double total = 500.0 + random.nextInt(2000);
            order.setTotalAmount(total);
            order.setPaidAmount(total); // Fully paid
            order.setBalanceDue(0.0);
            order.setStatus("COMPLETED");

            orderRepo.save(order);

            // Generate Commission for this order (Expense)
            if (d.getCommissionPercentage() != null && d.getCommissionPercentage() > 0) {
                CommissionLedger com = new CommissionLedger();
                com.setDoctor(d);
                com.setLabOrder(order);
                com.setTransactionDate(date.toLocalDate());
                double comAmount = total * (d.getCommissionPercentage() / 100.0);
                com.setCalculatedAmount(comAmount);

                // Randomly pay some commissions
                if (random.nextBoolean()) {
                    com.setStatus("PAID");
                    com.setPaidAmount(comAmount);
                    com.setPaymentDate(date.toLocalDate().plusDays(2));
                } else {
                    com.setStatus("PENDING");
                    com.setPaidAmount(0.0);
                }
                commissionRepo.save(com);
            }
        }

        // 3. Generate General Expenses (Utility Custom Payments)
        createExpense("Utility Bill", "Electricity Bill - Jan", 15000.0, 5);
        createExpense("Rent", "Lab Premises Rent - Jan", 50000.0, 20);
        createExpense("Office Supplies", "Stationery & Printing", 2500.0, 10);
        createExpense("Maintenance", "AC Repair", 3000.0, 12);

        // 4. Generate Supplier Payments
        if (!suppliers.isEmpty()) {
            for (int i = 0; i < 5; i++) {
                Supplier s = suppliers.get(random.nextInt(suppliers.size()));
                SupplierLedger sl = new SupplierLedger();
                sl.setSupplier(s);
                sl.setTransactionDate(LocalDateTime.now().minusDays(random.nextInt(20)).toLocalDate());
                sl.setBillAmount(5000.0 + random.nextInt(5000));
                sl.setPaidAmount(sl.getBillAmount()); // Fully paid
                sl.setBalanceDue(0.0);
                sl.setRemarks("Inventory Purchase #" + (1000 + i));

                supplierLedgerRepo.save(sl);
            }
        }

        System.out.println("✅ Financial Seeding Complete!");
    }

    private void createSupplier(String name, String phone) {
        Supplier s = new Supplier();
        s.setName(name);
        s.setContactNumber(phone);
        s.setAddress("City Industrial Area");
        s.setEmail("info@" + name.toLowerCase().replace(" ", "") + ".com");
        supplierRepo.save(s);
    }

    private void createExpense(String category, String desc, double amount, int daysAgo) {
        Payment p = new Payment();
        p.setType("EXPENSE");
        p.setCategory(category);
        p.setDescription(desc);
        p.setAmount(amount);
        p.setTransactionDate(LocalDateTime.now().minusDays(daysAgo));
        p.setPaymentMode("CASH");
        paymentRepo.save(p);
    }
}