package com.qdc.lims.service;

import com.qdc.lims.entity.SystemConfiguration;
import com.qdc.lims.repository.SystemConfigurationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to manage system configuration settings.
 * Caches settings in memory for performance.
 */
@Service
public class ConfigService {

    @Autowired
    private SystemConfigurationRepository configRepository;

    private Map<String, String> cache = new HashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
        ensureDefaults();
    }

    public void refreshCache() {
        List<SystemConfiguration> configs = configRepository.findAll();
        cache.clear();
        for (SystemConfiguration config : configs) {
            cache.put(config.getKey(), config.getValue());
        }
    }

    private void ensureDefaults() {
        createIfNotExists("CLINIC_NAME", "Quality Digital Clinic", "General");
        createIfNotExists("CLINIC_ADDRESS", "123 Healthcare Ave, Medical City", "General");
        createIfNotExists("CLINIC_PHONE", "+1 234 567 8900", "General");
        createIfNotExists("CLINIC_EMAIL", "info@qdc-lims.com", "General");

        createIfNotExists("CURRENCY_SYMBOL", "$", "Billing");
        createIfNotExists("TAX_RATE_PERCENT", "0.0", "Billing");

        createIfNotExists("REPORT_HEADER_TEXT", "Quality Digital Clinic Laboratory Report", "Reports");
        createIfNotExists("REPORT_FOOTER_TEXT", "This is a computer generated report and does not require a signature.",
                "Reports");
        createIfNotExists("REPORT_LOGO_PATH", "", "Reports");

        refreshCache();
    }

    private void createIfNotExists(String key, String defaultValue, String category) {
        if (!configRepository.existsById(key)) {
            SystemConfiguration config = new SystemConfiguration();
            config.setKey(key);
            config.setValue(defaultValue);
            config.setDescription("System Setting: " + key);
            config.setCategory(category);
            configRepository.save(config);
        }
    }

    public String get(String key) {
        return cache.getOrDefault(key, "");
    }

    public String get(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    public void set(String key, String value) {
        Optional<SystemConfiguration> opt = configRepository.findByKey(key);
        SystemConfiguration config;

        if (opt.isPresent()) {
            config = opt.get();
        } else {
            config = new SystemConfiguration();
            config.setKey(key);
            config.setDescription("Setting"); // Default description
            config.setCategory("General"); // Default category
        }

        config.setValue(value);
        configRepository.save(config);
        cache.put(key, value);
    }
}
