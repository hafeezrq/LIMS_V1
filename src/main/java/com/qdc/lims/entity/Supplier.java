package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entity representing a supplier from whom inventory items are purchased.
 */
@Entity
@Data
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName; // e.g. "Ali Distributors"

    private String contactPerson; // e.g. "Mr. Ali"

    private String mobile;

    private String email; // Add missing email field

    private String address;

    @Column(nullable = false)
    private boolean active = true;

    // Convenience getter to adhere to standard naming (Spring Boot usually maps
    // fields, but lombok @Data creates getCompanyName)
    // If other logic expects getName(), we can alias it.
    public String getName() {
        return companyName;
    }

    // Alias setters for convenience
    public void setName(String name) {
        this.companyName = name;
    }

    public void setContactNumber(String contactNumber) {
        this.mobile = contactNumber;
    }
}