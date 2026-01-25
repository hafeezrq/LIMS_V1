package com.qdc.lims.service;

import com.qdc.lims.entity.User;
import com.qdc.lims.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

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

    public User getUser(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}
