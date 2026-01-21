package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Represents the consumption recipe for a test.
 * E.g., A CBC test consumes 1 Vacuum Tube.
 */
@Entity
@Data
@Table(name = "test_recipes")
public class TestRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "test_id", nullable = false)
    private TestDefinition test;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(nullable = false)
    private Double quantity; // How much is consumed per test instance.

}
