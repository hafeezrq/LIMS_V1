package com.qdc.lims.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entity representing a laboratory test result, including value, abnormality,
 * remarks, and audit trail.
 */
@Entity
@Data
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    // Link back to the Order
    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore // Prevent infinite JSON loops
    private LabOrder labOrder;

    // Link to the Test Definition (to know min/max ranges)
    @ManyToOne
    @JoinColumn(name = "test_id")
    @NotNull(message = "Test definition is required")
    private TestDefinition testDefinition;

    // The data entered by the Technician
    private String resultValue;
    private boolean isAbnormal;
    private String remarks;

    // --- AUDIT TRAIL ---
    private String performedBy; // The Username (e.g., "labtech1")
    private java.time.LocalDateTime performedAt; // Exact timestamp

    @NotBlank(message = "Status is required")
    private String status; // "PENDING", "COMPLETED"

    public String getStatus() {
        return status;
    }

}
