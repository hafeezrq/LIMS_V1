package com.qdc.lims.repository;

import com.qdc.lims.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for key-value {@link SystemConfiguration} entries.
 */
@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, String> {

    /**
     * Retrieves a configuration entry by key.
     *
     * @param key configuration key
     * @return matching entry, if present
     */
    Optional<SystemConfiguration> findByKey(String key);
}
