package com.qdc.lims.util;

import com.qdc.lims.entity.*;
import com.qdc.lims.repository.*;
import com.qdc.lims.service.PatientService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Utility component for seeding the database with default data on application startup.
 * Seeds inventory items, doctors, test definitions, test consumption recipes, and sample patients.
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
    //private final LabInfoRepository labInfoRepo;

    /**
     * Constructs a DataSeeder with the required repositories and services.
     *
     * @param inventoryRepo repository for inventory items
     * @param doctorRepo repository for doctors
     * @param testRepo repository for test definitions
     * @param recipeRepo repository for test consumption recipes
     * @param patientRepo repository for patients
     * @param patientService service for patient registration logic
     * @param labInfoRepo repository for lab information
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
        //this.labInfoRepo = labInfoRepo;
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

        // --- C. TEST DEFINITIONS ---
        TestDefinition cbc = new TestDefinition();
        cbc.setTestName("Complete Blood Count (CBC)");
        cbc.setShortCode("CBC");
        cbc.setDepartment("Hematology");
        cbc.setPrice(650.0);
        cbc.setUnit("g/dL");
        cbc.setMinRange(11.0);
        cbc.setMaxRange(16.0);

        testRepo.save(cbc);

        TestDefinition glu = new TestDefinition();
        glu.setTestName("Serum Glucose (Random)");
        glu.setShortCode("GLU-R");
        glu.setDepartment("Biochemistry");
        glu.setPrice(250.0);
        glu.setUnit("mg/dL");
        glu.setMinRange(70.0);
        glu.setMaxRange(140.0);
        testRepo.save(glu);

        // --- D. RECIPES (Linking Test to Inventory) ---
        createRecipe(cbc, tube, 1.0);
        createRecipe(cbc, swab, 1.0);

        createRecipe(glu, strip, 1.0);
        createRecipe(glu, swab, 1.0);

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
     * @param qty the quantity of the item consumed per test
     */
    private void createRecipe(TestDefinition test, InventoryItem item, Double qty) {
        TestConsumption tc = new TestConsumption();
        tc.setTest(test);
        tc.setItem(item);
        tc.setQuantity(qty);
        recipeRepo.save(tc);
    }
}