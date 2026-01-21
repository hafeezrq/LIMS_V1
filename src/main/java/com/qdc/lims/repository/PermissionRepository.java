package com.qdc.lims.repository;

import com.qdc.lims.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

/**
 * Repository interface for Permission entities.
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    /**
     * Finds a permission by its name.
     *
     * @param name the permission name to search for (e.g., "PATIENT_CREATE")
     * @return an Optional containing the Permission if found
     */
    Optional<Permission> findByName(String name);

    /**
     * Finds all permissions in a specific category.
     *
     * @param category the permission category (e.g., "PATIENT", "TEST")
     * @return list of permissions in that category
     */
    List<Permission> findByCategory(String category);

    /**
     * Checks if a permission exists by name.
     *
     * @param name the permission name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);
}
