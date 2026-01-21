package com.qdc.lims.service;

import com.qdc.lims.dto.PurchaseRequest;
import com.qdc.lims.dto.PurchaseItemDTO; // Import the DTO
import com.qdc.lims.entity.*;
import com.qdc.lims.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Service for handling inventory purchases and supplier ledger updates.
 */
@Service
public class PurchaseService {

    private final InventoryItemRepository inventoryRepo;
    private final SupplierRepository supplierRepo;
    private final SupplierLedgerRepository ledgerRepo;

    /**
     * Constructs a PurchaseService with the required repositories.
     *
     * @param inventoryRepo repository for inventory items
     * @param supplierRepo repository for suppliers
     * @param ledgerRepo repository for supplier ledger entries
     */
    public PurchaseService(InventoryItemRepository inventoryRepo, SupplierRepository supplierRepo,
            SupplierLedgerRepository ledgerRepo) {
        this.inventoryRepo = inventoryRepo;
        this.supplierRepo = supplierRepo;
        this.ledgerRepo = ledgerRepo;
    }

    /**
     * Processes a purchase request, updates inventory, and creates supplier ledger entries.
     *
     * @param request the purchase request data
     */
    @Transactional
    public void processPurchase(PurchaseRequest request) {
        // --- 1. VALIDATION CHECK ---
        // If Invoice Number is provided, check for duplicates for THIS supplier
        if (request.invoiceNumber() != null && !request.invoiceNumber().trim().isEmpty()) {
            boolean exists = ledgerRepo.existsBySupplierIdAndInvoiceNumber(
                    request.supplierId(),
                    request.invoiceNumber());

            if (exists) {
                throw new RuntimeException("Duplicate Invoice: This invoice number already exists for this supplier.");
            }
        }

        Supplier supplier = supplierRepo.findById(request.supplierId()).orElseThrow();
        double totalBill = 0.0;

        // 1. Process Each Item
        for (PurchaseItemDTO itemDto : request.items()) {
            InventoryItem stockItem = inventoryRepo.findById(itemDto.itemId()).orElseThrow();

            // --- THE WAC MATH ---
            double oldStock = stockItem.getCurrentStock() != null ? stockItem.getCurrentStock() : 0.0;
            double oldCost = stockItem.getAverageCost() != null ? stockItem.getAverageCost() : 0.0;
            double oldTotalValue = oldStock * oldCost;

            double newQty = itemDto.quantity();
            double newCost = itemDto.costPrice();
            double newTotalValue = newQty * newCost;

            double finalQty = oldStock + newQty;
            double finalValue = oldTotalValue + newTotalValue;

            // Calculate New Average (Avoid divide by zero)
            double newAverageCost = (finalQty > 0) ? (finalValue / finalQty) : newCost;

            // --- UPDATE DB ---
            stockItem.setCurrentStock(finalQty);
            stockItem.setAverageCost(newAverageCost);

            // Optional: Set Preferred Supplier if not set
            if (stockItem.getPreferredSupplier() == null) {
                stockItem.setPreferredSupplier(supplier);
            }

            inventoryRepo.save(stockItem);

            totalBill += newTotalValue;
        }

        // 2. Create Financial Ledger Entry
        SupplierLedger ledger = new SupplierLedger();
        ledger.setSupplier(supplier);
        ledger.setTransactionDate(LocalDate.now());
        ledger.setDescription("Stock Purchase "
                + (request.invoiceNumber().isEmpty() ? "" : "(Inv: " + request.invoiceNumber() + ")"));
        ledger.setInvoiceNumber(request.invoiceNumber());
        ledger.setBillAmount(totalBill); // This increases what we owe

        ledgerRepo.save(ledger);

        // --- NEW: 3. Handle Immediate Payment (The Cash) ---
        if (request.amountPaidNow() != null && request.amountPaidNow() > 0) {
            SupplierLedger payLedger = new SupplierLedger();
            payLedger.setSupplier(supplier);
            payLedger.setTransactionDate(LocalDate.now());

            // Description: "Immediate Payment (Cash)"
            payLedger.setDescription("Immediate Payment (" + request.paymentMode() + ")");

            payLedger.setInvoiceNumber(request.invoiceNumber());
            payLedger.setBillAmount(0.0);
            payLedger.setPaidAmount(request.amountPaidNow()); // We paid this

            ledgerRepo.save(payLedger);
        }

    }
}