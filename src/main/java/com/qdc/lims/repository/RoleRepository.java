package com.qdc.lims.repository;

import com.qdc.lims.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository interface for Role entities.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {
    /**
     * Finds a role by its name.
     *
     * @param name the role name to search for (e.g., "ROLE_ADMIN")
     * @return an Optional containing the Role if found
     */
    Optional<Role> findByName(String name);

    /**
     * Checks if a role exists by name.
     *
     * @param name the role name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);
}
