-- 1️⃣ Drop old tables if they exist
DROP TABLE IF EXISTS panel_test CASCADE;
DROP TABLE IF EXISTS panel CASCADE;
DROP TABLE IF EXISTS test_definition CASCADE;
DROP TABLE IF EXISTS department CASCADE;

-- 2️⃣ Create department table
CREATE TABLE department (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    code VARCHAR(20),
    active BOOLEAN DEFAULT true
);

-- 3️⃣ Create test_definition table
CREATE TABLE test_definition (
    id BIGSERIAL PRIMARY KEY,
    test_name VARCHAR(255) NOT NULL,
    short_code VARCHAR(50),
    department_id INT NOT NULL REFERENCES department(id),
    unit VARCHAR(50),
    min_range NUMERIC(10,2),
    max_range NUMERIC(10,2),
    price NUMERIC(10,2),
    active BOOLEAN DEFAULT true
);

-- 4️⃣ Create panel table
CREATE TABLE panel (
    id SERIAL PRIMARY KEY,
    panel_name VARCHAR(100) NOT NULL,
    department_id INT REFERENCES department(id),
    active BOOLEAN DEFAULT true
);

-- 5️⃣ Create panel_test junction table
CREATE TABLE panel_test (
    panel_id INT REFERENCES panel(id),
    test_id BIGINT REFERENCES test_definition(id),
    PRIMARY KEY (panel_id, test_id)
);
