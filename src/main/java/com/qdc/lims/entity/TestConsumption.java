package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entity representing a recipe that links a test to inventory items and their required quantities.
 * This forms the basis for automatic inventory deduction when tests are ordered.
 */
@Entity
@Data
@Table(name = "test_consumption")
public class TestConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The Parent Test (e.g., "Glucose")
    @ManyToOne
    @JoinColumn(name = "test_id", nullable = false)
    private TestDefinition test;

    // The Ingredient (e.g., "Fluoride Tube")
    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    // The Quantity (e.g., 1.0 or 0.5)
    private Double quantity;
}