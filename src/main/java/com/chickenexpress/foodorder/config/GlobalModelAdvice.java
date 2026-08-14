package com.chickenexpress.foodorder.config;

import com.chickenexpress.foodorder.entity.User;
import com.chickenexpress.foodorder.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injects the current user into every model so that layouts can render
 * the avatar and display name without each controller needing to do it.
 *
 * Exposes:
 *   - {@code currentUser}            — the full {@link User} entity (or null if not logged in)
 *   - {@code currentProfileImageUrl} — kept for backward-compat with any existing template references
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final AuthService authService;

    public GlobalModelAdvice(AuthService authService) {
        this.authService = authService;
    }

    @ModelAttribute("currentUser")
    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserDetails ud)) {
            return null;
        }
        try {
            return authService.findByEmail(ud.getUsername());
        } catch (Exception e) {
            return null;
        }
    }

    /** Exposes the current user's DB id as a plain Long for the notification JS topic. */
    @ModelAttribute("currentUserId")
    public Long currentUserId() {
        User user = currentUser();
        return user != null ? user.getId() : null;
    }

    /** Kept for backward compatibility — delegates to currentUser. */
    @ModelAttribute("currentProfileImageUrl")
    public String currentProfileImageUrl() {
        User user = currentUser();
        return user != null ? user.getProfileImageUrl() : null;
    }
}
