package com.qdc.lims.service;

import com.qdc.lims.entity.Patient;
import com.qdc.lims.repository.PatientRepository;
import com.qdc.lims.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling patient registration and related logic.
 */
@Service
public class PatientService {

    private final PatientRepository repository;

    /**
     * Constructs a PatientService with the specified PatientRepository.
     *
     * @param repository the repository for patient entities
     */
    public PatientService(PatientRepository repository) {
        this.repository = repository;
    }

    /**
     * Registers a new patient, checks for CNIC uniqueness, and generates a unique MRN.
     *
     * @param patient the patient entity to register
     * @return the saved Patient entity
     */
    @Transactional
    public Patient registerPatient(Patient patient) {

        // 1. Handle Empty CNIC: Convert "" to null so the DB doesn't complain
        if (patient.getCnic() != null && patient.getCnic().trim().isEmpty()) {
            patient.setCnic(null);
        }

        // 2. Check if CNIC already exists (only if it's not null)
        if (patient.getCnic() != null) {
            if (repository.existsByCnic(patient.getCnic())) {
                throw new RuntimeException("Error: A patient with this CNIC already exists.");
            }
        }

        // 3. Generate a Unique MRN
        String newMrn = "";
        boolean isUnique = false;

        while (!isUnique) {
            newMrn = IdGenerator.generateMrn();
            if (!repository.existsByMrn(newMrn)) {
                isUnique = true;
            }
        }

        patient.setMrn(newMrn);
        return repository.save(patient);
    }
}