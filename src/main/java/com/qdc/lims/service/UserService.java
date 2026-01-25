package com.qdc.lims.service;

import com.qdc.lims.entity.User;
import com.qdc.lims.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for user management and authentication.
 * Implements Spring Security's UserDetailsService for authentication
 * integration.
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
    }

    /**
     * Load user by username for Spring Security authentication.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    /**
     * Authenticate a user with username and password.
     * Updates last login time on successful authentication.
     *
     * @param username    the username
     * @param rawPassword the plain text password
     * @return the authenticated User, or null if authentication fails
     */
    @Transactional
    public User authenticate(String username, String rawPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();

        // Check if user is active
        if (!user.isActive()) {
            throw new RuntimeException("User account is disabled");
        }

        // Verify password
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            return null;
        }

        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return user;
    }

    /**
     * Create a new user.
     *
     * @param user the user to create (password should be raw)
     * @return the saved user
     */
    @Transactional
    public User createUser(User user) {
        // Check if username already exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists: " + user.getUsername());
        }

        passwordPolicyService.validate(user.getPassword()).ifPresent(msg -> {
            throw new RuntimeException(msg);
        });

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    /**
     * Update an existing user.
     */
    @Transactional
    public User updateUser(User user) {
        String password = user.getPassword();
        if (password != null && !password.isBlank() && !isBcryptHash(password)) {
            passwordPolicyService.validate(password).ifPresent(msg -> {
                throw new RuntimeException(msg);
            });
            user.setPassword(passwordEncoder.encode(password));
        }
        return userRepository.save(user);
    }

    /**
     * Change a user's password.
     *
     * @param userId         the user ID
     * @param newRawPassword the new password (will be encoded)
     */
    @Transactional
    public void changePassword(Long userId, String newRawPassword) {
        passwordPolicyService.validate(newRawPassword).ifPresent(msg -> {
            throw new RuntimeException(msg);
        });
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    /**
     * Get all users.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get user by ID.
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Get user by username.
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Deactivate a user (soft delete).
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Activate a user.
     */
    @Transactional
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(true);
        userRepository.save(user);
    }

    /**
     * Delete a user permanently.
     */
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
