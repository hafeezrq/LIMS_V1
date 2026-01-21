package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entity representing a laboratory test definition, including pricing, ranges,
 * and consumption recipes.
 */
@Entity
@Data
@Table(name = "test_definitions")
public class TestDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String testName;

    private String shortCode;

    private Double price;

    private String unit;

    private String department;

    private Double minRange;
    private Double maxRange;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL)
    private java.util.List<ReferenceRange> ranges = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL)
    private java.util.List<TestRecipe> recipes = new java.util.ArrayList<>();

    // MAKE SURE THERE ARE NO INVENTORY FIELDS HERE!
}