package com.qdc.lims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Unified financial transaction view model used by reporting screens that merge
 * income and expense sources into a single timeline.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FinanceTransaction {

    /**
     * External source identifier (for example, an order number or payment id).
     */
    private String sourceId; // e.g., "ORD-101", "PAY-5"

    /**
     * Transaction date (normalized to a calendar day for reporting).
     */
    private LocalDate date;

    /**
     * Transaction type label such as {@code INCOME} or {@code EXPENSE}.
     */
    private String type; // "INCOME" or "EXPENSE"

    /**
     * Reporting category (for example, patient revenue, commissions, supplier).
     */
    private String category; // "Patient Revenue", "Doctor Commission", "Supplier", "OpEx"

    /**
     * Human-readable description for table rows and exports.
     */
    private String description;

    /**
     * Monetary amount for the transaction.
     */
    private Double amount;

    /**
     * Status indicator (for example, {@code COMPLETED} or {@code PENDING}).
     */
    private String status; // "COMPLETED", "PENDING"
}
