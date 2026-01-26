package com.qdc.lims.config;

import com.qdc.lims.entity.Permission;
import com.qdc.lims.entity.Role;
import com.qdc.lims.repository.PermissionRepository;
import com.qdc.lims.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Initializes default roles, permissions, and users if database is empty.
 */
@Configuration
public class AdminUserInitializer {

    @Bean
    public CommandLineRunner initializeData(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository) {

        return args -> {
            // Only initialize if no roles exist
            if (roleRepository.count() == 0) {
                System.out.println("🔧 Initializing Roles, Permissions, and Users...");

                // ============================
                // 1. CREATE PERMISSIONS
                // ============================
                Permission[] permissions = {
                        // Patient permissions
                        createPermission("PATIENT_CREATE", "Create new patient", "PATIENT"),
                        createPermission("PATIENT_VIEW", "View patient details", "PATIENT"),
                        createPermission("PATIENT_UPDATE", "Update patient info", "PATIENT"),
                        createPermission("PATIENT_DELETE", "Delete patient", "PATIENT"),

                        // Test/Order permissions
                        createPermission("ORDER_CREATE", "Create lab order", "ORDER"),
                        createPermission("ORDER_VIEW", "View lab orders", "ORDER"),
                        createPermission("ORDER_CANCEL", "Cancel lab order", "ORDER"),

                        // Result permissions
                        createPermission("RESULT_ENTRY", "Enter test results", "RESULT"),
                        createPermission("RESULT_VIEW", "View test results", "RESULT"),
                        createPermission("RESULT_APPROVE", "Approve test results", "RESULT"),

                        // Report permissions
                        createPermission("REPORT_GENERATE", "Generate reports", "REPORT"),
                        createPermission("REPORT_VIEW", "View reports", "REPORT"),
                        createPermission("REPORT_PRINT", "Print reports", "REPORT"),

                        // Inventory permissions
                        createPermission("INVENTORY_VIEW", "View inventory", "INVENTORY"),
                        createPermission("INVENTORY_MANAGE", "Manage inventory", "INVENTORY"),

                        // User management permissions
                        createPermission("USER_CREATE", "Create new user", "ADMIN"),
                        createPermission("USER_VIEW", "View users", "ADMIN"),
                        createPermission("USER_UPDATE", "Update user", "ADMIN"),
                        createPermission("USER_DELETE", "Delete user", "ADMIN"),
                        createPermission("USER_ASSIGN_ROLE", "Assign roles to user", "ADMIN"),

                        // System permissions
                        createPermission("SYSTEM_CONFIG", "Configure system settings", "ADMIN"),
                        createPermission("BACKUP_MANAGE", "Manage backups", "ADMIN")
                };

                permissionRepository.saveAll(Arrays.asList(permissions));
                System.out.println("  ✓ Created " + permissions.length + " permissions");

                // ============================
                // 2. CREATE ROLES
                // ============================

                // ADMIN Role - All permissions
                Role adminRole = new Role();
                adminRole.setName("ROLE_ADMIN");
                adminRole.setDescription("System Administrator - Full Access");
                adminRole.setPermissions(new HashSet<>(Arrays.asList(permissions)));
                roleRepository.save(adminRole);

                // RECEPTIONIST Role
                Role receptionRole = new Role();
                receptionRole.setName("ROLE_RECEPTION");
                receptionRole.setDescription("Reception Desk - Patient & Order Management");
                receptionRole.setPermissions(new HashSet<>(Arrays.asList(
                        permissions[0], permissions[1], permissions[2], // Patient create/view/update
                        permissions[4], permissions[5], // Order create/view
                        permissions[11], permissions[12] // Report view/print
                )));
                roleRepository.save(receptionRole);

                // LAB TECHNICIAN Role
                Role labRole = new Role();
                labRole.setName("ROLE_LAB");
                labRole.setDescription("Lab Technician - Result Entry");
                labRole.setPermissions(new HashSet<>(Arrays.asList(
                        permissions[1], // Patient view
                        permissions[5], // Order view
                        permissions[7], permissions[8], // Result entry/view
                        permissions[10], permissions[11], permissions[12] // Report generate/view/print
                )));
                roleRepository.save(labRole);

                // STAFF Role (Reception + Lab)
                Role staffRole = new Role();
                staffRole.setName("ROLE_STAFF");
                staffRole.setDescription("Staff - Reception and Lab access");
                staffRole.setPermissions(new HashSet<>(Arrays.asList(
                        permissions[0], permissions[1], permissions[2], // Patient create/view/update
                        permissions[4], permissions[5], // Order create/view
                        permissions[7], permissions[8], // Result entry/view
                        permissions[10], permissions[11], permissions[12] // Report generate/view/print
                )));
                roleRepository.save(staffRole);

                // PATHOLOGIST Role
                Role pathologistRole = new Role();
                pathologistRole.setName("ROLE_PATHOLOGIST");
                pathologistRole.setDescription("Pathologist - Result Approval");
                pathologistRole.setPermissions(new HashSet<>(Arrays.asList(
                        permissions[1], // Patient view
                        permissions[5], // Order view
                        permissions[7], permissions[8], permissions[9], // Result entry/view/approve
                        permissions[10], permissions[11], permissions[12] // Report generate/view/print
                )));
                roleRepository.save(pathologistRole);

                System.out.println("  ✓ Created 5 roles");

                System.out.println("  ✓ Roles and permissions created");
                System.out.println("\n✅ Initialization Complete!");
                System.out.println("No default users were created. Create the first admin on initial login.");
            }

            ensureStaffRole(roleRepository, permissionRepository);
        };
    }

    /**
     * Helper method to create a permission.
     */
    private Permission createPermission(String name, String description, String category) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(description);
        permission.setCategory(category);
        return permission;
    }

    private void ensureStaffRole(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        if (roleRepository.findByName("ROLE_STAFF").isPresent()) {
            return;
        }

        List<Permission> permissions = new ArrayList<>();
        permissions.add(ensurePermission(permissionRepository, "PATIENT_CREATE", "Create new patient", "PATIENT"));
        permissions.add(ensurePermission(permissionRepository, "PATIENT_VIEW", "View patient details", "PATIENT"));
        permissions.add(ensurePermission(permissionRepository, "PATIENT_UPDATE", "Update patient info", "PATIENT"));
        permissions.add(ensurePermission(permissionRepository, "ORDER_CREATE", "Create lab order", "ORDER"));
        permissions.add(ensurePermission(permissionRepository, "ORDER_VIEW", "View lab orders", "ORDER"));
        permissions.add(ensurePermission(permissionRepository, "RESULT_ENTRY", "Enter test results", "RESULT"));
        permissions.add(ensurePermission(permissionRepository, "RESULT_VIEW", "View test results", "RESULT"));
        permissions.add(ensurePermission(permissionRepository, "REPORT_GENERATE", "Generate reports", "REPORT"));
        permissions.add(ensurePermission(permissionRepository, "REPORT_VIEW", "View reports", "REPORT"));
        permissions.add(ensurePermission(permissionRepository, "REPORT_PRINT", "Print reports", "REPORT"));

        Role staffRole = new Role();
        staffRole.setName("ROLE_STAFF");
        staffRole.setDescription("Staff - Reception and Lab access");
        staffRole.setPermissions(new HashSet<>(permissions));
        roleRepository.save(staffRole);
    }

    private Permission ensurePermission(PermissionRepository permissionRepository, String name, String description,
            String category) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(createPermission(name, description, category)));
    }
}
