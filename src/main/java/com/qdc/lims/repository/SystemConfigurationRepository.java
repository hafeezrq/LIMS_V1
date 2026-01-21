package com.qdc.lims.repository;

import com.qdc.lims.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, String> {
    Optional<SystemConfiguration> findByKey(String key);
}
