package com.chickenexpress.foodorder.repository;

import com.chickenexpress.foodorder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entities.
 * Used by AuthService for login lookups and admin user management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Load a user by email — used by Spring Security's UserDetailsService. */
    Optional<User> findByEmail(String email);

    /** Check whether an email is already registered. */
    boolean existsByEmail(String email);
}
