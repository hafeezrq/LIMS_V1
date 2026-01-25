package com.qdc.lims.dto;

import java.util.List;

/**
 * DTO for creating a new lab order request.
 *
 * @param patientId the ID of the patient for the order
 * @param doctorId  the ID of the referring doctor (nullable for self/walk-in)
 * @param testIds   the list of test IDs to be ordered
 * @param panelIds  the list of panel IDs to be ordered
 * @param discount  the discount amount applied to the order
 * @param cashPaid  the amount of cash paid for the order
 */
public record OrderRequest(
                Long patientId,
                Long doctorId, // Can be null if Self/Walk-in
                List<Long> testIds,
                List<Integer> panelIds,
                Double discount,
                Double cashPaid) {
}