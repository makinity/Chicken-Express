package com.chickenexpress.foodorder.service;

import com.chickenexpress.foodorder.entity.User;
import com.chickenexpress.foodorder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthService.
 * Uses Mockito to mock UserRepository — no DB required.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    // ── loadUserByUsername ───────────────────────────────────────────────────

    @Test
    @DisplayName("loadUserByUsername: returns UserDetails when email exists")
    void loadUserByUsername_returnsUserDetails_whenEmailExists() {
        User user = new User("Test User", "test@example.com", "hashedpw");
        user.setRole("ROLE_CUSTOMER");
        user.setEnabled(true);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        var details = authService.loadUserByUsername("test@example.com");

        assertThat(details.getUsername()).isEqualTo("test@example.com");
        assertThat(details.getAuthorities())
            .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("loadUserByUsername: throws UsernameNotFoundException when email not found")
    void loadUserByUsername_throws_whenEmailNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loadUserByUsername("ghost@example.com"))
            .isInstanceOf(UsernameNotFoundException.class);
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: creates a new customer account with hashed password")
    void register_createsUser_withHashedPassword() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.register("Juan dela Cruz", "juan@example.com", "password123");

        assertThat(result.getFullName()).isEqualTo("Juan dela Cruz");
        assertThat(result.getEmail()).isEqualTo("juan@example.com");
        assertThat(result.getRole()).isEqualTo("ROLE_CUSTOMER");
        assertThat(result.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", result.getPassword())).isTrue();
    }

    @Test
    @DisplayName("register: throws when email already exists")
    void register_throws_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
            authService.register("Name", "existing@example.com", "pass"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }
}
