package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "test_categories")
@Data
public class TestCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(name = "is_active")
    private boolean active = true;
}
