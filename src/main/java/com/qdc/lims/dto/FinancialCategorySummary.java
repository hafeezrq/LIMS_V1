package com.qdc.lims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Aggregate row used by the financial queries screen to summarize counts and
 * totals by category and type.
 */
@Data
@AllArgsConstructor
public class FinancialCategorySummary {

    /**
     * Category label (for example, "Patient Services" or "Supplier Payments").
     */
    private String category;

    /**
     * Transaction type label such as {@code INCOME} or {@code EXPENSE}.
     */
    private String type; // INCOME or EXPENSE

    /**
     * Number of contributing records in the category.
     */
    private int count;

    /**
     * Summed monetary amount for the category.
     */
    private double totalAmount;
}
