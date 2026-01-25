package com.qdc.lims.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a laboratory order, including patient, doctor, billing,
 * and result details.
 */
@Entity
@Data
public class LabOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    // Link to the Patient
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    @NotNull(message = "Patient is required")
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id")
    private Doctor referringDoctor; // Visible to Receptionist ("Ref By: Dr. Bilal")

    private LocalDateTime orderDate;
    @NotBlank(message = "Status is required")
    private String status; // "PENDING", "COMPLETED"
    private Double totalAmount; // Calculated automatically

    // --- REPORT DELIVERY STATUS ---
    private boolean isReportDelivered = false; // False = In Lab/Rack, True = With Patient

    private LocalDateTime deliveryDate; // When was it handed over?

    // --- RESULT EDIT / REPRINT AUDIT ---
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean resultsEdited = false;
    private LocalDateTime resultsEditedAt;
    private String resultsEditedBy;
    private String resultsEditReason;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean reprintRequired = false;
    @Column(columnDefinition = "integer default 0")
    private Integer reprintCount = 0;
    private LocalDateTime lastReprintAt;
    private String lastReprintBy;

    // ---------- Update: To incorporate accounting ----------//
    private Double discountAmount = 0.0; // e.g. 100
    private Double taxAmount = 0.0; // (Optional, usually 0 in labs)
    private Double paidAmount = 0.0; // e.g. 500 (Patient paid half)
    private Double balanceDue = 0.0; // e.g. 400 (Remaining)

    // One Order = Many Tests (Results)
    // "CascadeType.ALL" means if we save the Order, it auto-saves the Result rows
    // too.
    // EAGER fetch to avoid LazyInitializationException in JavaFX controllers
    @OneToMany(mappedBy = "labOrder", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<LabResult> results = new ArrayList<>();

    /**
     * Sets the order date and initial status before persisting the entity.
     */
    @PrePersist
    protected void onCreate() {
        this.orderDate = LocalDateTime.now();
        this.status = "PENDING";
    }

    /**
     * Calculates the balance due before updating the entity.
     * Sets balanceDue = totalAmount - discountAmount - paidAmount.
     */
    @PreUpdate
    public void calculateBalance() {
        if (discountAmount == null)
            discountAmount = 0.0;
        if (paidAmount == null)
            paidAmount = 0.0;
        if (totalAmount == null)
            totalAmount = 0.0;

        this.balanceDue = totalAmount - discountAmount - paidAmount;
    }

    public long getPendingTestCount() {
        if (results == null)
            return 0;
        return results.stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.getStatus()))
                .count();
    }

}
