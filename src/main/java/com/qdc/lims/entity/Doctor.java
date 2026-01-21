package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entity representing a doctor and their commission details.
 */
@Entity
@Data
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., "Dr. Bilal Ahmed"

    private String clinicName; // e.g., "Bilal Clinic"

    private String mobile;

    // This is the PRIVATE rate (e.g., 10.0 for 10%)
    // The Receptionist UI will NOT show this field.
    private Double commissionPercentage = 0.0;

    @Column(nullable = false)
    private boolean active = true;
}