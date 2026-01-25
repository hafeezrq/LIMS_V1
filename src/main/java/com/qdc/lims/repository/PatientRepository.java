package com.qdc.lims.repository;

import com.qdc.lims.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Patient} registration and search operations.
 */
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Checks whether an MRN already exists.
     *
     * @param mrn medical record number
     * @return {@code true} if the MRN exists
     */
    boolean existsByMrn(String mrn);

    /**
     * Finds a patient by MRN.
     *
     * @param mrn medical record number
     * @return matching patient, if present
     */
    Optional<Patient> findByMrn(String mrn);

    /**
     * Checks whether a CNIC already exists.
     *
     * @param cnic national ID / CNIC
     * @return {@code true} if the CNIC exists
     */
    boolean existsByCnic(String cnic);

    /**
     * Searches patients by name, mobile number, or MRN using a case-insensitive
     * contains match.
     *
     * @param query free-text search input
     * @return matching patients
     */
    @Query("SELECT p FROM Patient p WHERE " +
            "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "p.mobileNumber LIKE CONCAT('%', :query, '%') OR " +
            "LOWER(p.mrn) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Patient> searchPatients(String query);
}
