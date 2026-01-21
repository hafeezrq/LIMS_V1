package com.qdc.lims.repository;

import com.qdc.lims.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for Supplier entities, providing standard CRUD operations.
 */
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
}
