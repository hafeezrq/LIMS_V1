package com.qdc.lims.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Entity representing an inventory item and its stock details.
 */
@Entity
@Data
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Item name is required")
    @Size(max = 255)
    private String itemName; // e.g., "Yellow Top Tube"

    private Double currentStock; // Using Double to handle liquids (e.g. 500.0 ml)

    private Double minThreshold; // e.g., 50.0. If stock drops below this, ALERT!

    private String unit; // e.g., "pcs", "ml", "strips"

    // The Weighted Average Cost (e.g. Rs 15.5 per unit)
    // Updated automatically every time we buy stock.
    private Double averageCost = 0.0;

    // The "Soft Link" to suggest a supplier
    @ManyToOne
    @JoinColumn(name = "preferred_supplier_id")
    private Supplier preferredSupplier;

    @Column(nullable = false)
    private boolean active = true;

}
