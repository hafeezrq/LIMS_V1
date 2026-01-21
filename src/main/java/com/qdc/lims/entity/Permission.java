package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a granular permission (e.g., PATIENT_CREATE,
 * TEST_APPROVE).
 * Permissions are assigned to roles.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "roles" })
@ToString(exclude = { "roles" })
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String name; // e.g., PATIENT_CREATE, TEST_APPROVE, REPORT_VIEW

    @Column(length = 200)
    private String description;

    @Column(length = 50)
    private String category; // PATIENT, TEST, REPORT, BILLING, ADMIN

    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;
}
