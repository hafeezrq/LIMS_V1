package com.qdc.lims.repository;

import com.qdc.lims.entity.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for LabResult entities, providing CRUD operations for lab test results.
 */
public interface LabResultRepository extends JpaRepository<LabResult, Long> {
}
