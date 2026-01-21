package com.qdc.lims.repository;

import com.qdc.lims.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository interface for User entities, providing authentication-related queries.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return an Optional containing the User if found
     */
    Optional<User> findByUsername(String username);
}
