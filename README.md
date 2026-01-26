# QDC LIMS

**IMPORTANT**
- Do not remove or change any working functionality without discussing it with the project owner and agreeing on the change.

**NOTE**
- Seed data runs only in the `dev` or `test` profile. Example: `SPRING_PROFILES_ACTIVE=dev`.
- Javadocs are generated on demand using: `./mvnw -Pdocs verify`
- The default profile is `prod`. Switch to dev/test with `SPRING_PROFILES_ACTIVE=dev` or `SPRING_PROFILES_ACTIVE=test`.

**Safe Data Entry Order (Empty Database)**
1. Create Admin (first run prompt).
2. Configure lab info (branding/header settings).
3. Create users.
4. Create Departments (Category Management).
5. Create Test Definitions (assign to departments).
6. Add Reference Ranges (recommended).
7. Add Doctors (optional).
8. Add Inventory + Test Recipes (optional).
9. Register Patients.
10. Create Lab Orders.
11. Enter Results in Lab.

**Backup Retention (Manual Management)**
- Automatic pruning is disabled (`qdc.backup.retention-days=0`).
- Backups are stored under the app data folder:
1. macOS: `~/Library/Application Support/QDC LIMS/Backups`
2. Windows: `%APPDATA%\\QDC LIMS\\Backups`
3. Linux: `~/.local/share/qdc-lims/Backups`
- To manage storage, periodically move older `.zip` backups to external storage or delete them manually.

**Production Build (Recommended)**
1. Build a native installer with bundled runtime:
```sh
./mvnw -DskipTests clean package jpackage
```
2. This uses the `badass-jlink` plugin and produces an installer under `target/`.

**Minimum Windows Requirements**
1. Windows 10 64-bit or Windows 11.
2. If distributing a plain JAR instead of the installer, Java 21 must be installed.
