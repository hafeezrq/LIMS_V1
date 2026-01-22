package com.qdc.lims.util;

import com.qdc.lims.entity.*;
import com.qdc.lims.repository.*;
import com.qdc.lims.service.PatientService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Utility component for seeding the database with default data on application
 * startup.
 * Seeds inventory items, doctors, test definitions, test consumption recipes,
 * and sample patients.
 * Only runs once on first startup when the database is empty.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final InventoryItemRepository inventoryRepo;
    private final DoctorRepository doctorRepo;
    private final TestDefinitionRepository testRepo;
    private final TestConsumptionRepository recipeRepo;
    // private final PatientRepository patientRepo;
    private final PatientService patientService;
    // private final LabInfoRepository labInfoRepo;

    /**
     * Constructs a DataSeeder with the required repositories and services.
     *
     * @param inventoryRepo  repository for inventory items
     * @param doctorRepo     repository for doctors
     * @param testRepo       repository for test definitions
     * @param recipeRepo     repository for test consumption recipes
     * @param patientRepo    repository for patients
     * @param patientService service for patient registration logic
     * @param labInfoRepo    repository for lab information
     */
    public DataSeeder(InventoryItemRepository inventoryRepo, DoctorRepository doctorRepo,
            TestDefinitionRepository testRepo, TestConsumptionRepository recipeRepo,
            PatientRepository patientRepo, PatientService patientService, LabInfoRepository labInfoRepo) {
        this.inventoryRepo = inventoryRepo;
        this.doctorRepo = doctorRepo;
        this.testRepo = testRepo;
        this.recipeRepo = recipeRepo;
        // this.patientRepo = patientRepo;
        this.patientService = patientService;
        // this.labInfoRepo = labInfoRepo;
    }

    /**
     * Runs the data seeding process on application startup.
     * Checks if the database is already populated; if not, seeds default data.
     *
     * @param args command line arguments (unused)
     * @throws Exception if an error occurs during seeding
     */
    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // 1. Check if DB is already full. If yes, stop.
        if (testRepo.count() > 0) {
            System.out.println("✅ Database already has data. Skipping Seeder.");
            return;
        }

        System.out.println("⚡ Seeding Default Data...");

        // --- A. INVENTORY ---
        InventoryItem tube = new InventoryItem();
        tube.setItemName("Purple Top Tube (EDTA)");
        tube.setCurrentStock(500.0);
        tube.setMinThreshold(50.0);
        tube.setUnit("pcs");
        inventoryRepo.save(tube);

        InventoryItem strip = new InventoryItem();
        strip.setItemName("Glucose Strip");
        strip.setCurrentStock(200.0);
        strip.setMinThreshold(20.0);
        strip.setUnit("pcs");
        inventoryRepo.save(strip);

        InventoryItem swab = new InventoryItem();
        swab.setItemName("Alcohol Swab");
        swab.setCurrentStock(1000.0);
        swab.setMinThreshold(100.0);
        swab.setUnit("pcs");
        inventoryRepo.save(swab);

        // --- B. DOCTORS ---
        Doctor doc1 = new Doctor();
        doc1.setName("Dr. Bilal Ahmed");
        doc1.setClinicName("City Hospital");
        doc1.setMobile("0300-1234567");
        doc1.setCommissionPercentage(10.0);
        doctorRepo.save(doc1);

        Doctor doc2 = new Doctor();
        doc2.setName("Dr. Sara Khan");
        doc2.setClinicName("Khan Clinic");
        doc2.setMobile("0321-7654321");
        doc2.setCommissionPercentage(15.0);
        doctorRepo.save(doc2);

        // --- C. TEST DEFINITIONS (Organized by Category) ---

        // === HEMATOLOGY ===
        TestDefinition cbc = createTest("Complete Blood Count (CBC)", "CBC", "Hematology", 650.0, "cells/mcL", 4500.0,
                11000.0);
        createTest("ESR (Erythrocyte Sedimentation Rate)", "ESR", "Hematology", 200.0, "mm/hr", 0.0, 20.0);
        createTest("Blood Group & Rh Factor", "BG-RH", "Hematology", 250.0, null, null, null);
        createTest("Hemoglobin (Hb)", "HB", "Hematology", 150.0, "g/dL", 12.0, 17.0);
        createTest("Platelet Count", "PLT", "Hematology", 200.0, "cells/mcL", 150000.0, 400000.0);
        createTest("PT/INR", "PT-INR", "Hematology", 450.0, "seconds", 11.0, 13.5);
        createTest("APTT", "APTT", "Hematology", 400.0, "seconds", 25.0, 35.0);
        createTest("Peripheral Blood Film", "PBF", "Hematology", 350.0, null, null, null);
        createTest("Reticulocyte Count", "RETIC", "Hematology", 300.0, "%", 0.5, 2.5);

        // === BIOCHEMISTRY ===
        TestDefinition bsf = createTest("Blood Sugar Fasting", "BSF", "Biochemistry", 150.0, "mg/dL", 70.0, 100.0);
        createTest("Blood Sugar Random", "BSR", "Biochemistry", 150.0, "mg/dL", 70.0, 140.0);
        createTest("Blood Sugar PP (Post Prandial)", "BSPP", "Biochemistry", 150.0, "mg/dL", 70.0, 140.0);
        createTest("HbA1c (Glycated Hemoglobin)", "HBA1C", "Biochemistry", 800.0, "%", 4.0, 5.6);
        createTest("Lipid Profile", "LIPID", "Biochemistry", 900.0, "mg/dL", null, null);
        createTest("Cholesterol Total", "CHOL", "Biochemistry", 250.0, "mg/dL", 0.0, 200.0);
        createTest("HDL Cholesterol", "HDL", "Biochemistry", 300.0, "mg/dL", 40.0, 60.0);
        createTest("LDL Cholesterol", "LDL", "Biochemistry", 300.0, "mg/dL", 0.0, 100.0);
        createTest("Triglycerides", "TG", "Biochemistry", 250.0, "mg/dL", 0.0, 150.0);
        createTest("Liver Function Test (LFT)", "LFT", "Biochemistry", 1200.0, null, null, null);
        createTest("SGPT (ALT)", "SGPT", "Biochemistry", 250.0, "U/L", 7.0, 56.0);
        createTest("SGOT (AST)", "SGOT", "Biochemistry", 250.0, "U/L", 10.0, 40.0);
        createTest("Alkaline Phosphatase", "ALP", "Biochemistry", 250.0, "U/L", 44.0, 147.0);
        createTest("Bilirubin Total", "BILI-T", "Biochemistry", 200.0, "mg/dL", 0.1, 1.2);
        createTest("Bilirubin Direct", "BILI-D", "Biochemistry", 200.0, "mg/dL", 0.0, 0.3);
        createTest("Renal Function Test (RFT/KFT)", "RFT", "Biochemistry", 800.0, null, null, null);
        createTest("Serum Uric Acid", "URIC", "Biochemistry", 300.0, "mg/dL", 3.5, 7.2);
        createTest("Serum Creatinine", "CREAT", "Biochemistry", 250.0, "mg/dL", 0.7, 1.3);
        createTest("Blood Urea", "UREA", "Biochemistry", 250.0, "mg/dL", 15.0, 45.0);
        createTest("BUN (Blood Urea Nitrogen)", "BUN", "Biochemistry", 250.0, "mg/dL", 7.0, 20.0);
        createTest("Serum Electrolytes (Na/K/Cl)", "ELEC", "Biochemistry", 600.0, "mEq/L", null, null);
        createTest("Serum Sodium", "NA", "Biochemistry", 250.0, "mEq/L", 136.0, 145.0);
        createTest("Serum Potassium", "K", "Biochemistry", 250.0, "mEq/L", 3.5, 5.0);
        createTest("Serum Calcium", "CA", "Biochemistry", 300.0, "mg/dL", 8.5, 10.5);
        createTest("Serum Phosphorus", "PHOS", "Biochemistry", 300.0, "mg/dL", 2.5, 4.5);
        createTest("Serum Magnesium", "MG", "Biochemistry", 350.0, "mg/dL", 1.7, 2.2);
        createTest("Total Protein", "TP", "Biochemistry", 200.0, "g/dL", 6.0, 8.3);
        createTest("Serum Albumin", "ALB", "Biochemistry", 200.0, "g/dL", 3.5, 5.0);
        createTest("Gamma GT (GGT)", "GGT", "Biochemistry", 300.0, "U/L", 9.0, 48.0);
        createTest("Amylase", "AMYL", "Biochemistry", 400.0, "U/L", 28.0, 100.0);
        createTest("Lipase", "LIPASE", "Biochemistry", 450.0, "U/L", 0.0, 160.0);

        // === SEROLOGY ===
        createTest("HIV I & II (Screening)", "HIV", "Serology", 500.0, null, null, null);
        createTest("HBsAg (Hepatitis B)", "HBSAG", "Serology", 400.0, null, null, null);
        createTest("Anti-HCV (Hepatitis C)", "HCV", "Serology", 500.0, null, null, null);
        createTest("VDRL/RPR (Syphilis)", "VDRL", "Serology", 300.0, null, null, null);
        createTest("Dengue NS1 Antigen", "DEN-NS1", "Serology", 800.0, null, null, null);
        createTest("Dengue IgG/IgM", "DEN-IGM", "Serology", 900.0, null, null, null);
        createTest("Typhidot (IgG/IgM)", "TYPHI", "Serology", 600.0, null, null, null);
        createTest("Widal Test", "WIDAL", "Serology", 350.0, null, null, null);
        createTest("CRP (C-Reactive Protein)", "CRP", "Serology", 450.0, "mg/L", 0.0, 10.0);
        createTest("CRP Quantitative", "CRP-Q", "Serology", 650.0, "mg/L", 0.0, 6.0);
        createTest("ASO Titre", "ASO", "Serology", 400.0, "IU/mL", 0.0, 200.0);
        createTest("RA Factor", "RA", "Serology", 450.0, "IU/mL", 0.0, 14.0);
        createTest("Brucella Antibodies", "BRUC", "Serology", 500.0, null, null, null);
        createTest("Malarial Parasite (MP)", "MP", "Serology", 200.0, null, null, null);
        createTest("ICT Malaria", "ICT-MAL", "Serology", 400.0, null, null, null);

        // === THYROID ===
        createTest("TSH (Thyroid Stimulating Hormone)", "TSH", "Thyroid", 500.0, "mIU/L", 0.4, 4.0);
        createTest("T3 (Triiodothyronine)", "T3", "Thyroid", 400.0, "ng/dL", 80.0, 200.0);
        createTest("T4 (Thyroxine)", "T4", "Thyroid", 400.0, "mcg/dL", 5.0, 12.0);
        createTest("Free T3 (FT3)", "FT3", "Thyroid", 500.0, "pg/mL", 2.3, 4.2);
        createTest("Free T4 (FT4)", "FT4", "Thyroid", 500.0, "ng/dL", 0.8, 1.8);
        createTest("Thyroid Profile (TSH, T3, T4)", "THYROID", "Thyroid", 1200.0, null, null, null);
        createTest("Anti-TPO Antibodies", "ATPO", "Thyroid", 900.0, "IU/mL", 0.0, 34.0);
        createTest("Anti-Thyroglobulin Ab", "ATG", "Thyroid", 900.0, "IU/mL", 0.0, 115.0);

        // === URINE ===
        createTest("Urine Routine Examination (R/E)", "URINE-RE", "Urine", 200.0, null, null, null);
        createTest("Urine Culture & Sensitivity", "URINE-CS", "Urine", 800.0, null, null, null);
        createTest("24-Hour Urine Protein", "URINE-24P", "Urine", 400.0, "mg/24hr", 0.0, 150.0);
        createTest("Urine for Microalbumin", "URINE-MA", "Urine", 500.0, "mg/L", 0.0, 30.0);
        createTest("Urine Pregnancy Test (UPT)", "UPT", "Urine", 200.0, null, null, null);
        createTest("Urine Drug Screen", "URINE-DRUG", "Urine", 1500.0, null, null, null);

        // === CARDIAC ===
        createTest("Troponin I", "TROP-I", "Cardiac", 1200.0, "ng/mL", 0.0, 0.04);
        createTest("Troponin T", "TROP-T", "Cardiac", 1200.0, "ng/L", 0.0, 14.0);
        createTest("CK-MB (Creatine Kinase-MB)", "CK-MB", "Cardiac", 600.0, "U/L", 0.0, 25.0);
        createTest("CK Total", "CK", "Cardiac", 400.0, "U/L", 30.0, 200.0);
        createTest("LDH (Lactate Dehydrogenase)", "LDH", "Cardiac", 350.0, "U/L", 140.0, 280.0);
        createTest("BNP (Brain Natriuretic Peptide)", "BNP", "Cardiac", 1500.0, "pg/mL", 0.0, 100.0);
        createTest("NT-proBNP", "NTPROBNP", "Cardiac", 1800.0, "pg/mL", 0.0, 125.0);
        createTest("D-Dimer", "DDIMER", "Cardiac", 1000.0, "ng/mL", 0.0, 500.0);
        createTest("Homocysteine", "HOMO", "Cardiac", 1200.0, "umol/L", 5.0, 15.0);

        // === HORMONES ===
        createTest("Vitamin D (25-OH)", "VITD", "Hormones", 1500.0, "ng/mL", 30.0, 100.0);
        createTest("Vitamin B12", "B12", "Hormones", 1200.0, "pg/mL", 200.0, 900.0);
        createTest("Serum Folate", "FOLATE", "Hormones", 800.0, "ng/mL", 2.7, 17.0);
        createTest("Serum Ferritin", "FERR", "Hormones", 600.0, "ng/mL", 12.0, 300.0);
        createTest("Serum Iron", "IRON", "Hormones", 400.0, "mcg/dL", 60.0, 170.0);
        createTest("TIBC (Total Iron Binding Capacity)", "TIBC", "Hormones", 450.0, "mcg/dL", 250.0, 370.0);
        createTest("Prolactin", "PRL", "Hormones", 700.0, "ng/mL", 2.0, 29.0);
        createTest("FSH (Follicle Stimulating Hormone)", "FSH", "Hormones", 600.0, "mIU/mL", null, null);
        createTest("LH (Luteinizing Hormone)", "LH", "Hormones", 600.0, "mIU/mL", null, null);
        createTest("Estradiol (E2)", "E2", "Hormones", 700.0, "pg/mL", null, null);
        createTest("Progesterone", "PROG", "Hormones", 700.0, "ng/mL", null, null);
        createTest("Testosterone Total", "TEST", "Hormones", 800.0, "ng/dL", 270.0, 1070.0);
        createTest("Testosterone Free", "TESTF", "Hormones", 1000.0, "pg/mL", 8.7, 25.1);
        createTest("Cortisol (Morning)", "CORT", "Hormones", 700.0, "mcg/dL", 6.0, 23.0);
        createTest("PSA (Prostate Specific Antigen)", "PSA", "Hormones", 900.0, "ng/mL", 0.0, 4.0);
        createTest("Beta-HCG (Pregnancy)", "BHCG", "Hormones", 800.0, "mIU/mL", null, null);
        createTest("Insulin Fasting", "INS", "Hormones", 900.0, "uIU/mL", 2.6, 24.9);
        createTest("Parathyroid Hormone (PTH)", "PTH", "Hormones", 1200.0, "pg/mL", 15.0, 65.0);

        // === SPECIAL TESTS ===
        createTest("Stool Routine Examination", "STOOL-RE", "Special Tests", 150.0, null, null, null);
        createTest("Stool Culture & Sensitivity", "STOOL-CS", "Special Tests", 800.0, null, null, null);
        createTest("Stool for Occult Blood", "FOB", "Special Tests", 200.0, null, null, null);
        createTest("H. Pylori Antigen (Stool)", "HPYLORI-S", "Special Tests", 800.0, null, null, null);
        createTest("H. Pylori Antibody (Blood)", "HPYLORI-B", "Special Tests", 700.0, null, null, null);
        createTest("Semen Analysis", "SEMEN", "Special Tests", 500.0, null, null, null);
        createTest("Pus Culture & Sensitivity", "PUS-CS", "Special Tests", 800.0, null, null, null);
        createTest("Blood Culture & Sensitivity", "BLOOD-CS", "Special Tests", 1200.0, null, null, null);
        createTest("AFB Smear (TB)", "AFB", "Special Tests", 400.0, null, null, null);
        createTest("Throat Swab Culture", "THROAT-CS", "Special Tests", 700.0, null, null, null);
        createTest("Nasal Swab (COVID-19 PCR)", "COVID-PCR", "Special Tests", 1500.0, null, null, null);
        createTest("COVID-19 Rapid Antigen", "COVID-RAT", "Special Tests", 500.0, null, null, null);

        // --- D. RECIPES (Linking Test to Inventory) ---
        createRecipe(cbc, tube, 1.0);
        createRecipe(cbc, swab, 1.0);

        createRecipe(bsf, strip, 1.0);
        createRecipe(bsf, swab, 1.0);

        // --- E. PATIENTS ---
        Patient p1 = new Patient();
        p1.setFullName("Ali Khan");
        p1.setAge(35);
        p1.setGender("Male");
        p1.setMobileNumber("0300-5555555");
        p1.setCity("Lahore");
        // Check if CNIC exists (optional logic not needed for seeder usually, but
        // handling generic registration)
        patientService.registerPatient(p1);

        Patient p2 = new Patient();
        p2.setFullName("Fatima Bibi");
        p2.setAge(28);
        p2.setGender("Female");
        p2.setMobileNumber("0321-9999999");
        p2.setCity("Karachi");
        patientService.registerPatient(p2);

        // --- F. LAB SETTINGS (Default) ---
        // if (labInfoRepo.count() == 0) {
        // LabInfo info = new LabInfo();
        // info.setId(1L);
        // info.setLabName("MY PATHOLOGY LAB");
        // info.setAddress("123 Main Street");
        // info.setCity("Lahore");
        // info.setPhoneNumber("0300-0000000");
        // info.setTagLine("Excellence in Diagnostics");
        // info.setEmail("contact@mylab.com");
        // info.setWebsite("www.mylab.com");

        // labInfoRepo.save(info);
        // }

        System.out.println("✅ Seeding Complete! System ready.");
    }

    /**
     * Creates a test consumption recipe entry linking a test to an inventory item.
     *
     * @param test the test definition
     * @param item the inventory item consumed by the test
     * @param qty  the quantity of the item consumed per test
     */
    private void createRecipe(TestDefinition test, InventoryItem item, Double qty) {
        TestConsumption tc = new TestConsumption();
        tc.setTest(test);
        tc.setItem(item);
        tc.setQuantity(qty);
        recipeRepo.save(tc);
    }

    /**
     * Creates and saves a test definition with the given parameters.
     *
     * @param name      full test name
     * @param shortCode short code for the test
     * @param category  test category for grouping
     * @param price     test price
     * @param unit      unit of measurement (nullable)
     * @param minRange  minimum reference range (nullable)
     * @param maxRange  maximum reference range (nullable)
     * @return the saved TestDefinition entity
     */
    private TestDefinition createTest(String name, String shortCode, String category,
            Double price, String unit, Double minRange, Double maxRange) {
        TestDefinition test = new TestDefinition();
        test.setTestName(name);
        test.setShortCode(shortCode);
        test.setCategory(category);
        test.setPrice(price);
        test.setUnit(unit);
        test.setMinRange(minRange);
        test.setMaxRange(maxRange);
        test.setActive(true);
        return testRepo.save(test);
    }
}