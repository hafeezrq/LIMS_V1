package com.qdc.lims.repository;

import com.qdc.lims.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Check if an MRN already exists (to prevent duplicates)
    boolean existsByMrn(String mrn);

    // Find a patient by their fancy ID
    Optional<Patient> findByMrn(String mrn);

    // Check if CNIC exists (to stop double registration)
    boolean existsByCnic(String cnic);

    // Search by Name OR Mobile OR MRN (Case insensitive)
    // SQL: SELECT * FROM patients WHERE lower(full_name) LIKE '%query%' OR
    // mobile_number LIKE '%query%' ...
    @Query("SELECT p FROM Patient p WHERE " +
            "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "p.mobileNumber LIKE CONCAT('%', :query, '%') OR " +
            "LOWER(p.mrn) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Patient> searchPatients(String query);

}