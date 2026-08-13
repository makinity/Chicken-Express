package com.chickenexpress.foodorder.service;

import com.chickenexpress.foodorder.entity.User;
import com.chickenexpress.foodorder.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Authentication and user registration service.
 *
 * Implements UserDetailsService so Spring Security can load users from the DB.
 * The "username" in this application is the user's email address.
 */
@Service
@Transactional
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── UserDetailsService ───────────────────────────────────────────────────

    /**
     * Called by Spring Security during form login.
     * Loads a user by email and wraps it in a Spring Security UserDetails object.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("No account found for: " + email));

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            user.isEnabled(),
            true, true, true,
            List.of(new SimpleGrantedAuthority(user.getRole()))
        );
    }

    // ── Registration ─────────────────────────────────────────────────────────

    /**
     * Register a new customer account.
     * Throws IllegalArgumentException if the email is already in use.
     *
     * @param fullName display name
     * @param email    login email (must be unique)
     * @param password plain-text password (will be BCrypt-hashed)
     * @return the saved User entity
     */
    public User register(String fullName, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = new User(fullName, email.toLowerCase().trim(), passwordEncoder.encode(password));
        user.setRole("ROLE_CUSTOMER");
        return userRepository.save(user);
    }

    // ── Profile ──────────────────────────────────────────────────────────────

    /**
     * Load the full User entity by email (for controllers that need more than UserDetails).
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    /**
     * Update a user's profile information.
     */
    public User updateProfile(Long userId, String fullName, String phone, String address, Double latitude, Double longitude) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address);
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        return userRepository.save(user);
    }

    /**
     * Update a user's profile image URL.
     */
    public User updateProfileImage(Long userId, String profileImageUrl) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setProfileImageUrl(profileImageUrl);
        return userRepository.save(user);
    }
}
