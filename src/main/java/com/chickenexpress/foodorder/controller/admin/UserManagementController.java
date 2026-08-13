package com.chickenexpress.foodorder.controller.admin;

import com.chickenexpress.foodorder.entity.User;
import com.chickenexpress.foodorder.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin user management: view all customers, activate/deactivate accounts.
 */
@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    private final UserRepository userRepository;

    public UserManagementController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/user_management";
    }

    @PostMapping("/{userId}/toggle-enabled")
    public String toggleEnabled(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setEnabled(!user.isEnabled());
        userRepository.save(user);

        String action = user.isEnabled() ? "activated" : "deactivated";
        redirectAttributes.addFlashAttribute("message",
            "Account for " + user.getFullName() + " has been " + action + ".");
        return "redirect:/admin/users";
    }
}
