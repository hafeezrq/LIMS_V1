package com.qdc.lims.config;

import com.qdc.lims.entity.TestConsumption;
import com.qdc.lims.entity.TestRecipe;
import com.qdc.lims.repository.TestConsumptionRepository;
import com.qdc.lims.repository.TestRecipeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Migrates legacy test recipe rows into the active test_consumption table.
 * This keeps inventory deduction aligned with the admin UI.
 */
@Configuration
public class TestRecipeMigrationConfig {

    @Bean
    public CommandLineRunner migrateLegacyTestRecipes(
            TestRecipeRepository legacyRecipeRepo,
            TestConsumptionRepository consumptionRepo) {
        return args -> {
            var legacyRecipes = legacyRecipeRepo.findAll();
            if (legacyRecipes.isEmpty()) {
                return;
            }

            int migrated = 0;
            for (TestRecipe legacy : legacyRecipes) {
                if (legacy.getTest() == null || legacy.getInventoryItem() == null) {
                    continue;
                }

                boolean exists = consumptionRepo
                        .findByTestAndItem(legacy.getTest(), legacy.getInventoryItem())
                        .isPresent();
                if (exists) {
                    continue;
                }

                TestConsumption tc = new TestConsumption();
                tc.setTest(legacy.getTest());
                tc.setItem(legacy.getInventoryItem());
                tc.setQuantity(legacy.getQuantity());
                consumptionRepo.save(tc);
                migrated++;
            }

            if (migrated > 0) {
                System.out.println("Migrated " + migrated + " legacy test recipe entries to test_consumption.");
            }
        };
    }
}

