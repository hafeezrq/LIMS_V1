package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Master data definition for a laboratory test, including pricing,
 * reference-range metadata, and relationships to departments and panels.
 */
@Entity
@Table(name = "test_definition")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDefinition {

    /**
     * Surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Display name of the test.
     */
    @Column(nullable = false)
    private String testName;

    /**
     * Optional short code used in order entry and reports.
     */
    private String shortCode;

    /**
     * Owning department/category for the test.
     */
    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Department department;

    /**
     * Measurement unit (nullable for qualitative tests).
     */
    private String unit;

    /**
     * Default minimum reference value.
     */
    private BigDecimal minRange;

    /**
     * Default maximum reference value.
     */
    private BigDecimal maxRange;

    /**
     * Price charged for the test.
     */
    private BigDecimal price;

    /**
     * Soft-active flag.
     */
    private Boolean active = true;

    /**
     * Panels that include this test.
     */
    @ManyToMany(mappedBy = "tests")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Panel> panels;

    /**
     * Reference ranges associated with the test.
     */
    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ReferenceRange> ranges;
}
