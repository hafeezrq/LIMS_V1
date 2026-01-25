package com.qdc.lims.repository;

import com.qdc.lims.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link Department} master data.
 */
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    /**
     * Finds a department by its unique name.
     *
     * @param name department name
     * @return matching department, if present
     */
    Optional<Department> findByName(String name);
}
