package com.qdc.lims.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Key-value system configuration entry used by the desktop settings screen.
 */
@Entity
@Table(name = "system_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfiguration {

    /**
     * Configuration key (primary key).
     */
    @Id
    @Column(name = "config_key", nullable = false, unique = true)
    private String key;

    /**
     * Configuration value (free-form string).
     */
    @Column(name = "config_value", length = 1000)
    private String value;

    /**
     * Human-readable description of the key.
     */
    @Column(name = "description")
    private String description;

    /**
     * Category grouping used by the settings UI (for example, General, Reports,
     * Billing).
     */
    @Column(name = "category")
    private String category; // "General", "Reports", "Billing"
}
