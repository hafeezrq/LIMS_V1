package com.qdc.lims.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Desktop-only crypto configuration.
 *
 * We don't run Spring Security's web filter chain in the desktop app, but we still
 * need BCrypt for password hashing/verification.
 */
@Configuration
public class DesktopCryptoConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
