# QDC-LIMS: Requirement Analysis & Architectural Review

## 1. Executive Summary

**QDC-LIMS** is a standalone, desktop-based Laboratory Information Management System designed for small-to-medium diagnostic centers. It utilizes a **Hybrid Architecture** combining the ease of **JavaFX** (Desktop UI) with the robustness of **Spring Boot** (Dependency Injection, Data Management).

**Current Status:** The application has moved beyond the prototype phase into the "Functional Beta" phase. The core modules (Admin, Financials, Configuration) are functional, but data fragmentation exists in the financial modules.

---

## 2. Functional Requirement Analysis

_A breakdown of what the system does and business needs._

### A. Core Modules (Business Logic)

1.  **Patient & Order Operations (Reception Dashboard)**
    - **Requirement:** Fast patient registration, test selection, and billing.
    - **Status:** Core flow exists.
    - **Gap:** Needs robust invoice printing and "Work List" generation for lab technicians.

2.  **Lab Operations (Lab Dashboard)**
    - **Requirement:** Result entry, verification, and report generation.
    - **Status:** Basic result entry logic is implied/stubbed.
    - **Gap:** Validation limits (Reference Ranges alerts) need to be strictly enforced during result entry.

3.  **Inventory Management**
    - **Requirement:** Tracking reagent consumption and Supplier management.
    - **Status:** Supplier ledger and basic item management exist.
    - **Gap:** **Auto-consumption.** Linking "Test Recipes" to "Inventory Stock" so that performing a "CBC" test automatically deducts reagents is defined but needs verifying in execution.

### B. Financial & Administration (The Complex Part)

1.  **Revenue & Reporting:**
    - **System:** Tracks income via `LabOrder`.
    - **Analysis:** Works well for Patient Income.
2.  **Doctor Commissions:**
    - **System:** Tracks specific payouts to doctors via `CommissionLedger`.
    - **Analysis:** Critical feature for business growth. Separation from general expenses is a good design choice.
3.  **General Expenses:**
    - **System:** Tracks rent/utilities via `Payment` entity.
    - **Observation:** This is where the architecture gets slightly repetitive (see Section 3).

---

## 3. Architectural Audit

_Technical evaluation of the code structure and design patterns._

### ✅ The Good (Strengths)

1.  **Spring Boot + JavaFX Integration:**
    - You are correctly using Spring's `ApplicationContext` to inject Services/Repositories into JavaFX Controllers. This is a very mature, professional pattern that avoids "Spaghetti code."
2.  **Separation of Concerns:**
    - FXML (View) is strictly separated from Controllers (Logic) and Entities (Data). This makes maintenance easier (as seen when we fixed the Tooltips without breaking Java logic).
3.  **Data Seeding Strategy:**
    - The `TestDataInitializer` is excellent. It ensures the system is testable immediately upon installation, significantly lowering the barrier for UAT (User Acceptance Testing).

### ⚠️ Areas for Improvement (Weaknesses/Risks)

1.  **Financial Data Fragmentation (Repetition):**
    - **Issue:** Financial data is scattered across three different tables: `LabOrder` (Income), `Payment` (Expenses), and `CommissionLedger` (Doctor Payouts).
    - **Consequence:** Building a simple "Profit & Loss" statement requires querying three different tables and manually stitching lists together in Java memory.
    - **Solution:** A **Unified Ledger Architecture**. Create a single `Transaction` table that links to these entities. Every time an Order is paid or an Expense is logged, it writes a line to the master `Transaction` table (Double-Entry Bookkeeping).

2.  **Database Concurrency (SQLite):**
    - **Issue:** The project currently uses SQLite (a single file).
    - **Risk:** If you install this on two computers (Reception and Admin office) pointing to the same network file, SQLite may lock or corrupt data during simultaneous writes.
    - **Recommendation:** For a detailed multi-user environment, plan to switch `application.properties` to use **MySQL** or **PostgreSQL** in the future. The current JPA/Hibernate setup makes this switch easy.

3.  **Controller Bloat (Lazy Loading):**
    - **Issue:** We recently faced `LazyInitializationExceptions`. We fixed this by making fetches `EAGER`.
    - **Risk:** As the database grows to 100,000+ orders, fetching everything `EAGER`ly will slow down the application.
    - **Recommendation:** In the future, specialized "Service Methods" should be written to fetch only specific data needed for reports, rather than loading entire object graphs.

---

## 4. Strategic Recommendations (Roadmap)

_Based on the analysis, here is the suggested path forward:_

### Phase A: Consolidation (Current Priority)

Do not build new features yet (except the critical gap below).

1.  **Implement "Financial Summary" (P&L):** Since the data is fragmented, build a specific Service (`FinanceService`) whose sole job is to query Orders, Payments, and Commissions and return a consolidated `ProfitLossDTO`.
2.  **Finalize "Financial Queries":** Use this unified logic to complete the placeholder menu item.

### Phase B: Operational Efficiency

1.  **Print Engine:** Develop the logic to generate PDF Patient Reports and Receipts. A LIMS is useless if it cannot print a result.
2.  **Result Entry Validation:** Ensure that if a result is `15.0` and the range is `10-12`, the UI flags it red immediately.

### Phase C: Infrastructure

1.  **Database Migration:** Move from H2/SQLite to a Dockerized MySQL instance for robust multi-user support.

---

### Conclusion

The architecture is **sound and professional**. The use of Spring Boot inside a JavaFX desktop app is a "Power Architecture" that offers the best of both worlds. The only architectural friction point is the **Financial Data Model**, which is slightly fragmented (separate silos for Orders vs Expenses) relative to a true accounting system, but is perfectly manageable for a LIMS via a Service layer aggregator.

**Verdict:** Proceed with confidence. Focus on the **Profit & Loss Service** next to tie all financial reporting together.
