package com.qdc.lims.repository;

import com.qdc.lims.entity.TestConsumption;
import com.qdc.lims.entity.TestDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository interface for TestConsumption entities, providing recipe lookup for inventory management.
 */
public interface TestConsumptionRepository extends JpaRepository<TestConsumption, Long> {
    /**
     * Finds all inventory consumption records (recipe) for a specific test.
     *
     * @param test the TestDefinition to lookup
     * @return list of TestConsumption entries showing required inventory items and quantities
     */
    List<TestConsumption> findByTest(TestDefinition test);
}
