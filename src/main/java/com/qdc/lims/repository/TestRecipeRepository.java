package com.qdc.lims.repository;

import com.qdc.lims.entity.TestRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link TestRecipe} definitions that describe inventory
 * consumption per test.
 */
@Repository
public interface TestRecipeRepository extends JpaRepository<TestRecipe, Long> {

    /**
     * Finds all recipe items for a given test id.
     *
     * @param testId test definition id
     * @return recipe items for the test
     */
    List<TestRecipe> findByTestId(Long testId);
}
