package com.qdc.lims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FinanceTransaction {
    private String sourceId; // e.g., "ORD-101", "PAY-5"
    private LocalDate date;
    private String type; // "INCOME" or "EXPENSE"
    private String category; // "Patient Revenue", "Doctor Commission", "Supplier", "OpEx"
    private String description;
    private Double amount;
    private String status; // "COMPLETED", "PENDING"
}
