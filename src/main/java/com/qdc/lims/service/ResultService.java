package com.qdc.lims.service;

import com.qdc.lims.dto.ResultEntryRequest;
import com.qdc.lims.entity.LabOrder;
import com.qdc.lims.entity.LabResult;
import com.qdc.lims.entity.TestDefinition;
import com.qdc.lims.repository.LabOrderRepository;
import com.qdc.lims.repository.LabResultRepository;

import com.qdc.lims.desktop.CurrentUserProvider;
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
            // Try to convert string "150" to number 150.0
            double val = Double.parseDouble(request.value());

            if (test.getMinRange() != null && test.getMaxRange() != null) {
                if (val < test.getMinRange()) {
                    result.setAbnormal(true);
                    result.setRemarks("LOW");
                } else if (val > test.getMaxRange()) {
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
     * Saves all lab results from a form, applies validation and audit logic, and updates order status.
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

        // 1. Get Current User (The Technician)
        String currentUser = currentUserProvider.getUsername();

        // Loop through the results submitted from the screen
        for (LabResult resultFromForm : orderForm.getResults()) {

            // Fetch the real result from DB to ensure we don't lose links
            LabResult dbResult = repository.findById(resultFromForm.getId()).orElseThrow();

            // Update the value
            String val = resultFromForm.getResultValue();
            dbResult.setResultValue(val);

            // --- AUDIT STAMP ---
            // Only update if the value changed or is new
            if (resultFromForm.getResultValue() != null && !resultFromForm.getResultValue().isEmpty()) {
                dbResult.setPerformedBy(currentUser);
                dbResult.setPerformedAt(LocalDateTime.now());
            }
            // ------------------------

            // Apply High/Low Logic
            TestDefinition test = dbResult.getTestDefinition();
            
            try {
                if (val != null && !val.isEmpty()) {
                    // 2. Parse Number
                    double numVal = Double.parseDouble(val);

                    // 3. Get Patient 
                    com.qdc.lims.entity.Patient patient = dbResult.getLabOrder().getPatient();

                    // 4. Find Matching Rule (The New Logic)
                    com.qdc.lims.entity.ReferenceRange matchingRule = null;
                    
                    // Check if ranges exist (avoid null pointer if list is empty)
                    if (test.getRanges() != null) {
                        for (com.qdc.lims.entity.ReferenceRange rule : test.getRanges()) {
                            // Check Gender ("Both", "Male", "Female")
                            boolean genderMatch = rule.getGender().equalsIgnoreCase("Both") 
                                               || rule.getGender().equalsIgnoreCase(patient.getGender());
                            
                            // Check Age
                            boolean ageMatch = patient.getAge() >= rule.getMinAge() 
                                            && patient.getAge() <= rule.getMaxAge();
    
                            if (genderMatch && ageMatch) {
                                matchingRule = rule;
                                break; // Found the specific rule for this person
                            }
                        }
                    }

                    // 5. Apply High/Low Logic based on the rule found
                    if (matchingRule != null) {
                        if (numVal < matchingRule.getMinVal()) {
                            dbResult.setAbnormal(true);
                            dbResult.setRemarks("LOW");
                        } else if (numVal > matchingRule.getMaxVal()) {
                            dbResult.setAbnormal(true);
                            dbResult.setRemarks("HIGH");
                        } else {
                            dbResult.setAbnormal(false);
                            dbResult.setRemarks("Normal");
                        }
                    } else {
                        // Fallback: If no specific Age/Gender rule found, assume Normal
                        // (Or you could check the old min/max fields if you kept them as backup)
                        dbResult.setAbnormal(false);
                        dbResult.setRemarks(""); 
                    }
                }
            } catch (NumberFormatException e) {
                // Handle Non-Numeric Results (Text like "Positive")
                dbResult.setAbnormal(false);
                dbResult.setRemarks("");
            }


            repository.save(dbResult);
        }

        // --- NEW LOGIC: Update Order Status ---
        // We assume if the tech clicked "Save", the order is done.
        com.qdc.lims.entity.LabOrder dbOrder = orderRepo.findById(orderForm.getId()).orElseThrow();
        dbOrder.setStatus("COMPLETED");
        orderRepo.save(dbOrder);

    }

}