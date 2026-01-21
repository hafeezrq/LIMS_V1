package com.qdc.lims.repository;

import com.qdc.lims.entity.TestRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRecipeRepository extends JpaRepository<TestRecipe, Long> {
    List<TestRecipe> findByTestId(Long testId);
}
