package com.qdc.lims.util;

import com.qdc.lims.entity.*;
import com.qdc.lims.repository.*;
import com.qdc.lims.service.PatientService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Seeds baseline master data into a fresh database on application startup.
 * <p>
 * The seeder is intentionally conservative: it exits early if any tests already
 * exist.
 */
@Component
@Profile({ "dev", "test" })
public class DataSeeder implements CommandLineRunner {

    private final InventoryItemRepository inventoryRepo;
    private final DoctorRepository doctorRepo;
    private final TestDefinitionRepository testRepo;
    private final TestConsumptionRepository recipeRepo;
    private final PatientService patientService;
    private final DepartmentRepository departmentRepo;

    /**
     * Creates the data seeder.
     *
     * @param inventoryRepo inventory repository
     * @param doctorRepo doctor repository
     * @param testRepo test definition repository
     * @param recipeRepo test consumption repository
     * @param patientRepo patient repository (currently unused but kept for wiring)
     * @param patientService patient service
     * @param departmentRepo department repository
     */
    public DataSeeder(InventoryItemRepository inventoryRepo,
            DoctorRepository doctorRepo,
            TestDefinitionRepository testRepo,
            TestConsumptionRepository recipeRepo,
            PatientRepository patientRepo,
            PatientService patientService,
            DepartmentRepository departmentRepo) {
        this.inventoryRepo = inventoryRepo;
        this.doctorRepo = doctorRepo;
        this.testRepo = testRepo;
        this.recipeRepo = recipeRepo;
        this.patientService = patientService;
        this.departmentRepo = departmentRepo;
    }

    /**
     * Runs the seed process once at startup if the database is empty.
     *
     * @param args command-line arguments
     */
    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // Stop early if the database already contains tests.
        if (testRepo.count() > 0) {
            System.out.println("✅ Database already has data. Skipping Seeder.");
            return;
        }

        System.out.println("⚡ Seeding Default Data...");

        // --- A. INVENTORY ---
        saveInventory("Purple Top Tube (EDTA)", 500.0, 50.0, "pcs");
        saveInventory("Glucose Strip", 200.0, 20.0, "pcs");
        saveInventory("Alcohol Swab", 1000.0, 100.0, "pcs");

        // --- B. DOCTORS ---
        saveDoctor("Dr. Bilal Ahmed", "City Hospital", "0300-1234567", 10.0);
        saveDoctor("Dr. Sara Khan", "Khan Clinic", "0321-7654321", 15.0);

        // --- C. TEST DEFINITIONS ---
        TestDefinition cbc = createTest("Complete Blood Count (CBC)", "CBC", "Hematology", 650.0, "cells/mcL",
                4500.0, 11000.0);
        createTest("ESR (Erythrocyte Sedimentation Rate)", "ESR", "Hematology", 200.0, "mm/hr", 0.0, 20.0);
        createTest("Blood Group & Rh Factor", "BG-RH", "Hematology", 250.0, null, null, null);
        createTest("Hemoglobin (Hb)", "HB", "Hematology", 150.0, "g/dL", 12.0, 17.0);
        createTest("Platelet Count", "PLT", "Hematology", 200.0, "cells/mcL", 150000.0, 400000.0);

        TestDefinition bsf = createTest("Blood Sugar Fasting", "BSF", "Biochemistry", 150.0, "mg/dL", 70.0,
                100.0);
        createTest("Blood Sugar Random", "BSR", "Biochemistry", 150.0, "mg/dL", 70.0, 140.0);
        createTest("HbA1c", "HBA1C", "Biochemistry", 800.0, "%", 4.0, 5.6);
        createTest("Lipid Profile", "LIPID", "Biochemistry", 900.0, "mg/dL", null, null);
        createTest("Cholesterol Total", "CHOL", "Biochemistry", 250.0, "mg/dL", 0.0, 200.0);
        createTest("Creatinine", "CREAT", "Biochemistry", 250.0, "mg/dL", 0.7, 1.3);
        createTest("Liver Function Test (LFT)", "LFT", "Biochemistry", 1200.0, null, null, null);
        createTest("ALT (SGPT)", "SGPT", "Biochemistry", 250.0, "U/L", 7.0, 56.0);

        createTest("HIV I & II", "HIV", "Serology", 500.0, null, null, null);
        createTest("HBsAg", "HBSAG", "Serology", 400.0, null, null, null);
        createTest("HCV", "HCV", "Serology", 500.0, null, null, null);
        createTest("Typhidot", "TYPHI", "Serology", 600.0, null, null, null);
        createTest("Dengue NS1", "DEN-NS1", "Serology", 800.0, null, null, null);

        createTest("TSH", "TSH", "Thyroid", 500.0, "mIU/L", 0.4, 4.0);
        createTest("T3", "T3", "Thyroid", 400.0, "ng/dL", 80.0, 200.0);
        createTest("T4", "T4", "Thyroid", 400.0, "mcg/dL", 5.0, 12.0);

        createTest("Urine Routine", "URINE-RE", "Urine", 200.0, null, null, null);
        createTest("Urine Culture", "URINE-CS", "Urine", 800.0, null, null, null);
        createTest("Urine Pregnancy (UPT)", "UPT", "Urine", 200.0, null, null, null);

        // --- D. RECIPES ---
        InventoryItem tube = inventoryRepo.findByItemName("Purple Top Tube (EDTA)").orElse(null);
        InventoryItem strip = inventoryRepo.findByItemName("Glucose Strip").orElse(null);
        InventoryItem swab = inventoryRepo.findByItemName("Alcohol Swab").orElse(null);

        if (tube != null && swab != null) {
            createRecipe(cbc, tube, 1.0);
            createRecipe(cbc, swab, 1.0);
        }
        if (strip != null && bsf != null && swab != null) {
            createRecipe(bsf, strip, 1.0);
            createRecipe(bsf, swab, 1.0);
        }

        // --- E. PATIENTS ---
        registerPatient("Ali Khan", 35, "Male", "0300-5555555", "Lahore");
        registerPatient("Fatima Bibi", 28, "Female", "0321-9999999", "Karachi");

        System.out.println("✅ Seeding Complete! System ready.");
    }

    /**
     * Creates and stores a default inventory item.
     */
    private void saveInventory(String name, Double stock, Double threshold, String unit) {
        InventoryItem item = new InventoryItem();
        item.setItemName(name);
        item.setCurrentStock(stock);
        item.setMinThreshold(threshold);
        item.setUnit(unit);
        inventoryRepo.save(item);
    }

    /**
     * Creates and stores a default doctor record.
     */
    private void saveDoctor(String name, String clinic, String mobile, Double comm) {
        Doctor doc = new Doctor();
        doc.setName(name);
        doc.setClinicName(clinic);
        doc.setMobile(mobile);
        doc.setCommissionPercentage(comm);
        doctorRepo.save(doc);
    }

    /**
     * Registers a demo patient using the patient service (to reuse MRN logic).
     */
    private void registerPatient(String name, int age, String gender, String mobile, String city) {
        Patient p = new Patient();
        p.setFullName(name);
        p.setAge(age);
        p.setGender(gender);
        p.setMobileNumber(mobile);
        p.setCity(city);
        patientService.registerPatient(p);
    }

    /**
     * Creates a recipe item linking a test to an inventory item.
     */
    private void createRecipe(TestDefinition test, InventoryItem item, Double qty) {
        if (test == null || item == null) {
            return;
        }
        TestConsumption tc = new TestConsumption();
        tc.setTest(test);
        tc.setItem(item);
        tc.setQuantity(qty);
        recipeRepo.save(tc);
    }

    /**
     * Creates a test definition and ensures its department exists.
     */
    private TestDefinition createTest(String name, String shortCode, String categoryName,
            Double price, String unit, Double minRange, Double maxRange) {

        TestDefinition test = new TestDefinition();
        test.setTestName(name);
        test.setShortCode(shortCode);

        Department dept = departmentRepo.findByName(categoryName).orElseGet(() -> {
            Department d = new Department();
            d.setName(categoryName);
            String code = categoryName.length() > 3 ? categoryName.substring(0, 3).toUpperCase()
                    : categoryName.toUpperCase();
            d.setCode(code);
            d.setActive(true);
            return departmentRepo.save(d);
        });

        test.setDepartment(dept);

        if (price != null) {
            test.setPrice(BigDecimal.valueOf(price));
        }
        test.setUnit(unit);
        if (minRange != null && minRange > 0) {
            test.setMinRange(BigDecimal.valueOf(minRange));
        }
        if (maxRange != null && maxRange > 0) {
            test.setMaxRange(BigDecimal.valueOf(maxRange));
        }

        test.setActive(true);
        return testRepo.save(test);
    }
}
