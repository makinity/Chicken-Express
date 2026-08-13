package com.chickenexpress.foodorder.config;

import com.chickenexpress.foodorder.entity.User;
import com.chickenexpress.foodorder.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injects the current user's profileImageUrl into every model so that
 * the navbar can render the avatar without each controller needing to do it.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final AuthService authService;

    public GlobalModelAdvice(AuthService authService) {
        this.authService = authService;
    }

    @ModelAttribute("currentProfileImageUrl")
    public String currentProfileImageUrl() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserDetails ud)) {
            return null;
        }
        try {
            User user = authService.findByEmail(ud.getUsername());
            return user.getProfileImageUrl();
        } catch (Exception e) {
            return null;
        }
    }
}
