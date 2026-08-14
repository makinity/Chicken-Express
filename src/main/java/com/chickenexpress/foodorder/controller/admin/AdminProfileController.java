package com.chickenexpress.foodorder.controller.admin;

import com.chickenexpress.foodorder.entity.User;
import com.chickenexpress.foodorder.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * Admin profile page — update name, email, avatar, and password.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>GET  /admin/profile               — show profile page</li>
 *   <li>POST /admin/profile               — update name + email</li>
 *   <li>POST /admin/profile/avatar        — upload profile picture</li>
 *   <li>POST /admin/profile/password      — change password</li>
 * </ul>
 */
@Controller
@RequestMapping("/admin/profile")
public class AdminProfileController {

    private static final String UPLOAD_DIR = "uploads/avatars/";
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB
    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final AuthService authService;

    public AdminProfileController(AuthService authService) {
        this.authService = authService;
    }

    // ── GET ──────────────────────────────────────────────────────────────────

    @GetMapping
    public String profilePage(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = authService.findByEmail(principal.getUsername());
        model.addAttribute("user", user);
        return "admin/profile";
    }

    // ── POST /admin/profile — update name + email ─────────────────────────

    @PostMapping
    public String updateProfile(@AuthenticationPrincipal UserDetails principal,
                                @RequestParam String fullName,
                                @RequestParam String email,
                                RedirectAttributes redirectAttrs) {

        if (fullName == null || fullName.isBlank()) {
            redirectAttrs.addFlashAttribute("errorMessage", "Full name cannot be empty.");
            return "redirect:/admin/profile";
        }
        if (email == null || email.isBlank()) {
            redirectAttrs.addFlashAttribute("errorMessage", "Email cannot be empty.");
            return "redirect:/admin/profile";
        }

        try {
            User user = authService.findByEmail(principal.getUsername());
            authService.updateAdminProfile(user.getId(), fullName, email);
            redirectAttrs.addFlashAttribute("toastSuccess", "Profile updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/profile";
    }

    // ── POST /admin/profile/avatar ────────────────────────────────────────

    @PostMapping("/avatar")
    public String uploadAvatar(@AuthenticationPrincipal UserDetails principal,
                               @RequestParam("avatarFile") MultipartFile file,
                               RedirectAttributes redirectAttrs) {

        if (file.isEmpty()) {
            redirectAttrs.addFlashAttribute("errorMessage", "Please select an image to upload.");
            return "redirect:/admin/profile";
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            redirectAttrs.addFlashAttribute("errorMessage", "Image must be smaller than 2 MB.");
            return "redirect:/admin/profile";
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Only JPEG, PNG, WebP, or GIF images are allowed.");
            return "redirect:/admin/profile";
        }

        try {
            User user = authService.findByEmail(principal.getUsername());

            // Delete old avatar to avoid orphaned files
            deleteAvatarFile(user.getProfileImageUrl());

            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);
            String ext      = getExtension(file.getOriginalFilename(), contentType);
            String filename = UUID.randomUUID() + ext;
            Files.copy(file.getInputStream(), uploadPath.resolve(filename));

            authService.updateProfileImage(user.getId(), "/uploads/avatars/" + filename);
            redirectAttrs.addFlashAttribute("toastSuccess", "Profile photo updated.");

        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Upload failed — please try again.");
        }

        return "redirect:/admin/profile";
    }

    // ── POST /admin/profile/password ──────────────────────────────────────

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal UserDetails principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttrs) {

        if (!newPassword.equals(confirmPassword)) {
            redirectAttrs.addFlashAttribute("passwordError", "New passwords do not match.");
            return "redirect:/admin/profile#password";
        }

        try {
            User user = authService.findByEmail(principal.getUsername());
            authService.changePassword(user.getId(), currentPassword, newPassword);
            redirectAttrs.addFlashAttribute("toastSuccess", "Password changed successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("passwordError", e.getMessage());
        }

        return "redirect:/admin/profile#password";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void deleteAvatarFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            String relative = imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;
            Path file = Paths.get(relative).normalize();
            Path root = Paths.get(UPLOAD_DIR).normalize().toAbsolutePath();
            if (file.toAbsolutePath().startsWith(root)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ignored) {}
    }

    private String getExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return switch (contentType) {
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif"  -> ".gif";
            default           -> ".jpg";
        };
    }
}
