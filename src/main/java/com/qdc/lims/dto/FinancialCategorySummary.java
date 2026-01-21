package com.qdc.lims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FinancialCategorySummary {
    private String category;
    private String type; // INCOME or EXPENSE
    private int count;
    private double totalAmount;
}
