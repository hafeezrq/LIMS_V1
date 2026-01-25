package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * Entity representing a financial ledger entry for tracking purchases from and
 * payments to suppliers.
 */
@Entity
@Data
@Table(name = "supplier_ledger")
public class SupplierLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    private LocalDate transactionDate;

    private String description; // e.g. "Inv-999 Purchase" or "Cash Payment"

    private String invoiceNumber; // Optional, for cross-checking paper bills
    private LocalDate invoiceDate;
    private LocalDate dueDate;

    // Money Logic
    private Double billAmount = 0.0; // Money we OWE (Credit) - Increases Balance
    private Double paidAmount = 0.0; // Money we PAID (Debit) - Decreases Balance

    // Add missing balanceDue field - not persisted, calculated/tracked?
    // Actually, for a ledger, usually balance is calculated on the fly or
    // snapshotted.
    // But TestDataInitializer tries to set it. Let's add it.
    private Double balanceDue = 0.0;

    // Add missing setRemarks method (mapped to description or separate?)
    // TestDataInitializer calls setRemarks. Let's add a separate remarks field.
    private String remarks;

    /**
     * Sets the transaction date before persisting the entity if not already set.
     */
    @PrePersist
    protected void onCreate() {
        if (transactionDate == null)
            transactionDate = LocalDate.now();
    }
}
