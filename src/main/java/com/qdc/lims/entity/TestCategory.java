package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Legacy/alternate test category entity. The current UI primarily uses
 * {@link Department}, but this table may still be referenced by reporting or
 * migration scripts.
 */
@Entity
@Table(name = "test_categories")
@Data
public class TestCategory {

    /**
     * Surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique category name.
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Optional category description.
     */
    private String description;

    /**
     * Soft-active flag.
     */
    @Column(name = "is_active")
    private boolean active = true;
}
