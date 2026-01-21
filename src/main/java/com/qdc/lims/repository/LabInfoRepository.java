package com.qdc.lims.repository;

import com.qdc.lims.entity.LabInfo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for LabInfo entity, providing access to laboratory configuration settings.
 * The system typically maintains a single LabInfo record with ID 1.
 */
public interface LabInfoRepository extends JpaRepository<LabInfo, Long> {
}
