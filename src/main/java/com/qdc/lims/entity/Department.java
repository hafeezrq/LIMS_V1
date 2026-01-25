package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Department (also used as a "category" in the UI) that groups related
 * {@link TestDefinition} and {@link Panel} entries.
 */
@Entity
@Table(name = "department")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    /**
     * Surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Display name of the department (unique).
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Short code used in reports or lookups.
     */
    private String code;

    /**
     * Soft-active flag.
     */
    private Boolean active = true;

    /**
     * All tests defined under this department.
     */
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<TestDefinition> testDefinitions;

    /**
     * All panels grouped under this department.
     */
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Panel> panels;
}
