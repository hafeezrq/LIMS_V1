package com.qdc.lims.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entity representing laboratory information and contact details.
 */
@Entity
@Data
@Table(name = "lab_info")
public class LabInfo {

    @Id
    private Long id = 1L; // We force ID to be 1 so there's only ever one record

    private String labName;
    private String address;
    private String phoneNumber;
    private String city;

    // Optional: Slogan or Tagline
    private String tagLine;
    private String email;
    private String website; // Can be a URL like "www.qdc.com" or "fb.com/qdc"

}