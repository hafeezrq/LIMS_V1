-- ---------------------------------------------------------
-- 1. CLEANUP (The order is strict: Children first, Parents last)
-- ---------------------------------------------------------

-- 1. Financial & Inventory dependencies
DELETE FROM commission_ledger;
DELETE FROM test_consumption;

-- 2. Transactional Data (Results & Orders)
DELETE FROM lab_result;
DELETE FROM lab_order; 

-- 3. Configuration Links (Many-to-Many tables)
DELETE FROM panel_test;

-- 4. Master Definitions (The parents)
DELETE FROM panel;
DELETE FROM test_definition;
DELETE FROM department;

-- ---------------------------------------------------------
-- 2. INSERT DATA
-- ---------------------------------------------------------

-- Departments
INSERT INTO department (id, name, code, active) VALUES
(1, 'Hematology', 'HEM', true),
(2, 'Biochemistry', 'BIO', true),
(3, 'Microbiology', 'MIC', true),
(4, 'Immunology', 'IMM', true),
(5, 'Serology', 'SER', true),
(6, 'Urine', 'URI', true);

-- Atomic tests 
INSERT INTO test_definition (id, test_name, short_code, department_id, unit, min_range, max_range, price, active) VALUES
(1, 'Hemoglobin', 'HB', 1, 'g/dL', 12.0, 17.5, 5.00, true),
(2, 'Hematocrit', 'HCT', 1, '%', 36, 53, 5.00, true),
(3, 'RBC Count', 'RBC', 1, '10^6/uL', 4.1, 5.9, 5.00, true),
(4, 'WBC Count', 'WBC', 1, '10^3/uL', 4.0, 11.0, 5.00, true),
(5, 'Platelet Count', 'PLT', 1, '10^3/uL', 150, 450, 5.00, true),
(6, 'Fasting Glucose', 'FBS', 2, 'mg/dL', 70, 99, 4.00, true),
(7, 'Creatinine', 'CRE', 2, 'mg/dL', 0.6, 1.3, 4.00, true),
(8, 'ALT (SGPT)', 'ALT', 2, 'U/L', 7, 56, 6.00, true),
(9, 'AST (SGOT)', 'AST', 2, 'U/L', 10, 40, 6.00, true),
(10, 'Total Bilirubin', 'TBIL', 2, 'mg/dL', 0.1, 1.2, 6.00, true),
(11, 'Direct Bilirubin', 'DBIL', 2, 'mg/dL', 0.0, 0.3, 6.00, true),
(12, 'HIV I & II', 'HIV', 5, null, null, null, 10.00, true),
(13, 'HBsAg', 'HBSAG', 5, null, null, null, 10.00, true),
(14, 'Urine Routine', 'URINE-RE', 6, null, null, null, 4.00, true),
(15, 'Urine Culture', 'URINE-CS', 6, null, null, null, 8.00, true);

-- Panels
INSERT INTO panel (id, panel_name, department_id, active) VALUES
(1, 'CBC', 1, true),
(2, 'LFT', 2, true),
(3, 'Renal Function Test', 2, true),
(4, 'Hepatitis Panel', 5, true),
(5, 'HIV Panel', 5, true),
(6, 'Urine Panel', 6, true);

-- Link tests to panels
INSERT INTO panel_test (panel_id, test_id) VALUES
(1, 1),  -- Hemoglobin in CBC
(1, 2),  -- Hematocrit in CBC
(1, 3),  -- RBC Count in CBC
(1, 4),  -- WBC Count in CBC
(1, 5),  -- Platelet Count in CBC
(2, 6),  -- Fasting Glucose in LFT
(2, 7),  -- Creatinine in LFT
(3, 8),  -- ALT in LFT
(3, 9),  -- AST in LFT
(3, 10), -- Total Bilirubin in LFT
(3, 11), -- Direct Bilirubin in LFT
(4, 13), -- HBsAg in Hepatitis Panel
(5, 12), -- HIV I & II in HIV Panel
(6, 14), -- Urine Routine in Urine Panel
(6, 15); -- Urine Culture in Urine Panel

-- ---------------------------------------------------------
-- 3. RESET SEQUENCES
-- ---------------------------------------------------------
SELECT setval('department_id_seq', (SELECT MAX(id) FROM department));
SELECT setval('test_definition_id_seq', (SELECT MAX(id) FROM test_definition));
SELECT setval('panel_id_seq', (SELECT MAX(id) FROM panel));