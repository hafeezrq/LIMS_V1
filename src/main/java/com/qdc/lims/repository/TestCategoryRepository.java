package com.qdc.lims.repository;

import com.qdc.lims.entity.TestCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestCategoryRepository extends JpaRepository<TestCategory, Long> {
    Optional<TestCategory> findByName(String name);
}
