package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * A billable panel that groups multiple {@link TestDefinition} items under a
 * single name.
 */
@Entity
@Table(name = "panel")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Panel {

    /**
     * Surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Display name of the panel.
     */
    @Column(nullable = false)
    private String panelName;

    /**
     * Owning department (optional in the current schema).
     */
    @ManyToOne
    @JoinColumn(name = "department_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Department department;

    /**
     * Soft-active flag.
     */
    private Boolean active = true;

    /**
     * Tests that belong to this panel.
     */
    @ManyToMany
    @JoinTable(
            name = "panel_test",
            joinColumns = @JoinColumn(name = "panel_id"),
            inverseJoinColumns = @JoinColumn(name = "test_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<TestDefinition> tests;
}
