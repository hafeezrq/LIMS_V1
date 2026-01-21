package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity representing a general financial payment or expense.
 * Tracks incoming/outgoing payments not directly tied to Lab Order or
 * Commission/Supplier ledgers.
 * Can be used for:
 * - Operating Expenses (Rent, Utility Bills, Salaries)
 * - Miscellaneous Income
 * - Custom transactions
 */
@Entity
@Data
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Type of transaction
    @Column(nullable = false)
    private String type; // "INCOME", "EXPENSE", "ADJUSTMENT"

    @Column(nullable = false)
    private String category; // "RENT", "SALARY", "UTILITIES", "MISC", "REFUND"

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double amount;

    // Rename or Alias paymentMode to match setPaymentMode usage
    private String paymentMethod; // "CASH", "BANK_TRANSFER", "CHEQUE"

    public void setPaymentMode(String mode) {
        this.paymentMethod = mode;
    }

    public String getPaymentMode() {
        return this.paymentMethod;
    }

    private String referenceNumber; // Bank tx ID or Check #

    private LocalDateTime transactionDate;

    // Optional user tracing - who recorded this?
    @ManyToOne
    @JoinColumn(name = "recorded_by_user_id")
    private User recordedBy;

    // Optional links if we expand later
    private String remarks;

    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
    }
}
