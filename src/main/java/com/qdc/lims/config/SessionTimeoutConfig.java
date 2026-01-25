package com.qdc.lims.config;

import com.qdc.lims.ui.SessionManager;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionTimeoutConfig {

    @Value("${qdc.session.timeout:30}")
    private long sessionTimeoutMinutes;

    @PostConstruct
    public void configureSessionTimeout() {
        SessionManager.setSessionTimeoutMinutes(sessionTimeoutMinutes);
    }
}
