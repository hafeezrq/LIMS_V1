package com.qdc.lims.service;

import com.qdc.lims.entity.User;
import com.qdc.lims.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Authentication helper service used by the desktop login flow. It validates
 * account state, verifies the password, and records the last login timestamp.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates the authentication service.
     *
     * @param userRepository user repository
     * @param passwordEncoder password encoder for verifying stored hashes
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates a user by username and raw password.
     *
     * @param username username to authenticate
     * @param password raw password to verify
     * @return {@code true} if authentication succeeds
     */
    public boolean authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (!user.isActive() || !user.isAccountNonExpired() || !user.isAccountNonLocked()
                || !user.isCredentialsNonExpired()) {
            return false;
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return false;
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return true;
    }

    /**
     * Retrieves a user by username.
     *
     * @param username username to look up
     * @return the matching user, or {@code null} if none exists
     */
    public User getUser(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}
