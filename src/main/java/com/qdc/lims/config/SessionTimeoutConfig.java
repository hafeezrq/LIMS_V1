package com.qdc.lims.config;

import com.qdc.lims.ui.SessionManager;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that applies the session timeout to the desktop UI
 * {@link SessionManager} at startup.
 */
@Configuration
public class SessionTimeoutConfig {

    @Value("${qdc.session.timeout:30}")
    private long sessionTimeoutMinutes;

    /**
     * Pushes the configured timeout (in minutes) into the global session manager
     * after dependency injection completes.
     */
    @PostConstruct
    public void configureSessionTimeout() {
        SessionManager.setSessionTimeoutMinutes(sessionTimeoutMinutes);
    }
}
