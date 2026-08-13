package com.chickenexpress.foodorder.controller.customer;

import com.chickenexpress.foodorder.service.AuthService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles login and registration pages.
 *
 * Spring Security handles the actual /login POST authentication;
 * this controller only serves the GET pages and the registration POST.
 */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("error", "Invalid email or password.");
        if (logout != null) model.addAttribute("message", "You have been logged out.");
        return "customer/login";
    }

    // ── Registration ─────────────────────────────────────────────────────────

    @GetMapping("/register")
    public String registerPage() {
        return "customer/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam @NotBlank String fullName,
                           @RequestParam @Email @NotBlank String email,
                           @RequestParam @Size(min = 8) String password,
                           @RequestParam String confirmPassword,
                           RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/register";
        }

        try {
            authService.register(fullName, email, password);
            redirectAttributes.addFlashAttribute("message",
                "Account created successfully. Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }
}
