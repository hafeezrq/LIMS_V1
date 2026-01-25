package com.qdc.lims.service;

import com.qdc.lims.dto.ResultEntryRequest;
import com.qdc.lims.entity.LabOrder;
import com.qdc.lims.entity.LabResult;
import com.qdc.lims.entity.TestDefinition;
import com.qdc.lims.repository.LabOrderRepository;
import com.qdc.lims.repository.LabResultRepository;

import com.qdc.lims.ui.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for handling lab result entry, validation, and saving logic.
 */
@Service
public class ResultService {

    private final LabResultRepository repository;
    private final CurrentUserProvider currentUserProvider;

    @Autowired
    private LabOrderRepository orderRepo;

    /**
     * Constructs a ResultService with the specified LabResultRepository.
     *
     * @param repository repository for lab results
     */
    public ResultService(LabResultRepository repository, CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Enters a single lab result, applies auto-validation logic, and saves it.
     *
     * @param request the result entry request data
     * @return the saved LabResult entity
     */
    @Transactional
    public LabResult enterResult(ResultEntryRequest request) {
        // 1. Fetch the specific result row
        LabResult result = repository.findById(request.resultId())
                .orElseThrow(() -> new RuntimeException("Result ID not found"));

        TestDefinition test = result.getTestDefinition();

        // 2. Save the value
        result.setResultValue(request.value());

        // 3. Auto-Validation Logic
        try {
            java.math.BigDecimal val = new java.math.BigDecimal(request.value());

            if (test.getMinRange() != null && test.getMaxRange() != null) {
                if (val.compareTo(test.getMinRange()) < 0) {
                    result.setAbnormal(true);
                    result.setRemarks("LOW");
                } else if (val.compareTo(test.getMaxRange()) > 0) {
                    result.setAbnormal(true);
                    result.setRemarks("HIGH");
                } else {
                    result.setAbnormal(false);
                    result.setRemarks("Normal");
                }
            }
        } catch (NumberFormatException e) {
            // If the result is text (e.g., "Positive"), we can't check ranges
            result.setAbnormal(false);
        }

        return repository.save(result);
    }

    /**
     * Saves all lab results from a form, applies validation and audit logic, and
     * updates order status.
     *
     * @param orderForm the LabOrder containing results to save
     */
    @Transactional
    public void saveResultsFromForm(LabOrder orderForm) {

        // 1. Security Check
        LabOrder labOrder = orderRepo.findById(orderForm.getId())
                .orElseThrow(() -> new RuntimeException("The Order not found"));
        if (labOrder.isReportDelivered()) {
            throw new RuntimeException("⛔ ILLEGAL ACTION: Cannot modify results after report delivery.");
        }

        // 1. Get Current User
        String currentUser = currentUserProvider.getUsername();

        // Loop through the results submitted from the screen
        for (LabResult resultFromForm : orderForm.getResults()) {

            // Fetch the real result from DB
            LabResult dbResult = repository.findById(resultFromForm.getId()).orElseThrow();
            String val = resultFromForm.getResultValue();

            // =========================================================
            // FIX START: Only update if the new value is NOT Empty/Null
            // =========================================================
            if (val != null && !val.trim().isEmpty()) {

                dbResult.setResultValue(val);

                // Audit Stamp
                dbResult.setPerformedBy(currentUser);
                dbResult.setPerformedAt(LocalDateTime.now());

                // --- Apply High/Low Logic (Moved inside the check) ---
                TestDefinition test = dbResult.getTestDefinition();

                try {
                    // 2. Parse Number
                    java.math.BigDecimal numVal = new java.math.BigDecimal(val);

                    // 3. Get Patient
                    com.qdc.lims.entity.Patient patient = dbResult.getLabOrder().getPatient();

                    // 4. Find Matching Rule
                    com.qdc.lims.entity.ReferenceRange matchingRule = null;

                    if (test.getRanges() != null) {
                        for (com.qdc.lims.entity.ReferenceRange rule : test.getRanges()) {
                            boolean genderMatch = rule.getGender().equalsIgnoreCase("Both")
                                    || rule.getGender().equalsIgnoreCase(patient.getGender());

                            boolean ageMatch = patient.getAge() >= rule.getMinAge()
                                    && patient.getAge() <= rule.getMaxAge();

                            if (genderMatch && ageMatch) {
                                matchingRule = rule;
                                break;
                            }
                        }
                    }

                    // 5. Apply High/Low Logic
                    if (matchingRule != null) {
                        if (numVal.compareTo(matchingRule.getMinVal()) < 0) {
                            dbResult.setAbnormal(true);
                            dbResult.setRemarks("LOW");
                        } else if (numVal.compareTo(matchingRule.getMaxVal()) > 0) {
                            dbResult.setAbnormal(true);
                            dbResult.setRemarks("HIGH");
                        } else {
                            dbResult.setAbnormal(false);
                            dbResult.setRemarks("Normal");
                        }
                    } else {
                        dbResult.setAbnormal(false);
                        dbResult.setRemarks("");
                    }
                } catch (NumberFormatException e) {
                    // Handle Non-Numeric Results (Text like "Positive")
                    dbResult.setAbnormal(false);
                    dbResult.setRemarks("");
                }

                // Save only the modified result
                repository.save(dbResult);
            }
            // =========================================================
            // FIX END
            // =========================================================
        }

        // --- LOGIC UPDATE: Only Mark "COMPLETED" if ALL tests are done ---
        // (Optional improvement: Prevent partial orders being marked complete)
        LabOrder dbOrder = orderRepo.findById(orderForm.getId()).orElseThrow();

        boolean allTestsDone = dbOrder.getResults().stream()
                .allMatch(r -> r.getResultValue() != null && !r.getResultValue().trim().isEmpty());

        if (allTestsDone) {
            dbOrder.setStatus("COMPLETED");
        } else {
            dbOrder.setStatus("IN_PROGRESS");
        }
        orderRepo.save(dbOrder);
    }

    /**
     * Saves edits to results for a completed order and records audit metadata.
     *
     * @param orderForm   the LabOrder containing edited results
     * @param editReason  reason for editing (required if already delivered)
     */
    @Transactional
    public void saveEditedResults(LabOrder orderForm, String editReason) {
        LabOrder labOrder = orderRepo.findById(orderForm.getId())
                .orElseThrow(() -> new RuntimeException("The Order not found"));

        if (!"COMPLETED".equals(labOrder.getStatus())) {
            throw new RuntimeException("Only completed orders can be edited here.");
        }

        if (labOrder.isReportDelivered()) {
            if (editReason == null || editReason.trim().isEmpty()) {
                throw new RuntimeException("Edit reason is required after report delivery.");
            }
        }

        String currentUser = currentUserProvider.getUsername();

        for (LabResult resultFromForm : orderForm.getResults()) {
            LabResult dbResult = repository.findById(resultFromForm.getId()).orElseThrow();
            String val = resultFromForm.getResultValue();

            if (val != null && !val.trim().isEmpty()) {
                dbResult.setResultValue(val);
                dbResult.setPerformedBy(currentUser);
                dbResult.setPerformedAt(LocalDateTime.now());

                TestDefinition test = dbResult.getTestDefinition();
                try {
                    java.math.BigDecimal numVal = new java.math.BigDecimal(val);
                    com.qdc.lims.entity.Patient patient = dbResult.getLabOrder().getPatient();

                    com.qdc.lims.entity.ReferenceRange matchingRule = null;
                    if (test.getRanges() != null) {
                        for (com.qdc.lims.entity.ReferenceRange rule : test.getRanges()) {
                            boolean genderMatch = rule.getGender().equalsIgnoreCase("Both")
                                    || rule.getGender().equalsIgnoreCase(patient.getGender());
                            boolean ageMatch = patient.getAge() >= rule.getMinAge()
                                    && patient.getAge() <= rule.getMaxAge();
                            if (genderMatch && ageMatch) {
                                matchingRule = rule;
                                break;
                            }
                        }
                    }

                    if (matchingRule != null) {
                        if (numVal.compareTo(matchingRule.getMinVal()) < 0) {
                            dbResult.setAbnormal(true);
                            dbResult.setRemarks("LOW");
                        } else if (numVal.compareTo(matchingRule.getMaxVal()) > 0) {
                            dbResult.setAbnormal(true);
                            dbResult.setRemarks("HIGH");
                        } else {
                            dbResult.setAbnormal(false);
                            dbResult.setRemarks("Normal");
                        }
                    } else {
                        dbResult.setAbnormal(false);
                        dbResult.setRemarks("");
                    }
                } catch (NumberFormatException e) {
                    dbResult.setAbnormal(false);
                    dbResult.setRemarks("");
                }

                repository.save(dbResult);
            }
        }

        labOrder.setResultsEdited(true);
        labOrder.setResultsEditedAt(LocalDateTime.now());
        labOrder.setResultsEditedBy(currentUser);
        labOrder.setResultsEditReason(editReason);

        if (labOrder.isReportDelivered()) {
            labOrder.setReprintRequired(true);
        }

        orderRepo.save(labOrder);
    }

}
