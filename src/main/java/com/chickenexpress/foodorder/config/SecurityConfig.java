package com.chickenexpress.foodorder.config;

import com.chickenexpress.foodorder.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security configuration for ChickenExpress.
 *
 * Role hierarchy:
 *   ROLE_ADMIN  → full access to /admin/** and customer-facing pages
 *   ROLE_CUSTOMER → access to customer-facing pages only
 *
 * Public access (no login required):
 *   /, /menu, /register, /login, /css/**, /js/**, /images/**,
 *   /webhooks/paymongo (machine-to-machine — verified by signature)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final AuthService authService;
    private final RoleBasedSuccessHandler roleBasedSuccessHandler;

    // @Lazy breaks the circular dependency: SecurityConfig → AuthService → PasswordEncoder → SecurityConfig
    public SecurityConfig(@Lazy AuthService authService, RoleBasedSuccessHandler roleBasedSuccessHandler) {
        this.authService = authService;
        this.roleBasedSuccessHandler = roleBasedSuccessHandler;
    }

    // ── Password Encoder ────────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── Authentication Provider ─────────────────────────────────────────────

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(authService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── Security Filter Chain ───────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())

            // ── Authorization rules ──────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Static assets — always public
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                // WebSocket / SockJS endpoint — must be public so the HTTP upgrade handshake works
                .requestMatchers("/ws/**").permitAll()

                // Uploaded files (product images, user avatars) — public read access
                .requestMatchers("/uploads/**").permitAll()

                // Error page (Spring Boot forwards here on exceptions)
                .requestMatchers("/error").permitAll()

                // Public pages
                .requestMatchers("/", "/menu", "/menu/**").permitAll()
                .requestMatchers("/login", "/register").permitAll()

                // PayMongo webhook — machine-to-machine (signature verified in controller)
                .requestMatchers("/webhooks/paymongo").permitAll()

                // Chatbot API — public so guests can also use it
                .requestMatchers("/api/chat").permitAll()

                // Chatbot API — public so guests can also use it
                .requestMatchers("/api/chat").permitAll()

                // Checkout success/cancel pages — accessible after redirect
                .requestMatchers("/checkout/success", "/checkout/cancel").permitAll()

                // Admin area — ROLE_ADMIN only
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Customer-only routes — ROLE_CUSTOMER only (admins manage via /admin/**)
                .requestMatchers("/cart/**", "/checkout/**", "/orders/**").hasRole("CUSTOMER")

                // All other pages require authentication
                .anyRequest().authenticated()
            )

            // ── Form login ───────────────────────────────────────────────
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(roleBasedSuccessHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )

            // ── Logout ───────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // ── CSRF ─────────────────────────────────────────────────────
            // Disable CSRF only for the webhook endpoint (PayMongo sends raw POST).
            // All browser-facing forms remain CSRF-protected.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/webhooks/paymongo")
            );

        return http.build();
    }
}
