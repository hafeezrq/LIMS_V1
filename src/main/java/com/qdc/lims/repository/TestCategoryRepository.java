package com.qdc.lims.repository;

import com.qdc.lims.entity.TestCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link TestCategory} entries.
 */
@Repository
public interface TestCategoryRepository extends JpaRepository<TestCategory, Long> {

    /**
     * Finds a category by its unique name.
     *
     * @param name category name
     * @return matching category, if present
     */
    Optional<TestCategory> findByName(String name);
}
