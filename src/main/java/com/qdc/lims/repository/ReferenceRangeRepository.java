package com.qdc.lims.repository;

import com.qdc.lims.entity.ReferenceRange;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for ReferenceRange entities, providing CRUD operations
 * for test reference ranges.
 */
public interface ReferenceRangeRepository extends JpaRepository<ReferenceRange, Long> {
    /**
     * Find reference ranges by TestDefinition ID.
     */
    java.util.List<ReferenceRange> findByTestId(Long testId);
}
