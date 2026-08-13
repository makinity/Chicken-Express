package com.chickenexpress.foodorder.controller.customer;

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
 * Handles the customer profile page — view profile info and upload avatar.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final String UPLOAD_DIR = "uploads/avatars/";
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final AuthService authService;

    public ProfileController(AuthService authService) {
        this.authService = authService;
    }

    // ── GET /profile ─────────────────────────────────────────────────────────

    @GetMapping
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = authService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "My Profile — ChickenExpress");
        return "customer/profile";
    }

    // ── POST /profile (update info) ───────────────────────────────────────────

    @PostMapping
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) Double latitude,
                                @RequestParam(required = false) Double longitude,
                                RedirectAttributes redirectAttrs) {
        User user = authService.findByEmail(userDetails.getUsername());
        authService.updateProfile(user.getId(), fullName, phone, address, latitude, longitude);
        redirectAttrs.addFlashAttribute("successMessage", "Profile updated successfully.");
        return "redirect:/profile";
    }

    // ── POST /profile/avatar (upload image) ───────────────────────────────────

    @PostMapping("/avatar")
    public String uploadAvatar(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam("avatarFile") MultipartFile file,
                               RedirectAttributes redirectAttrs) {

        if (file.isEmpty()) {
            redirectAttrs.addFlashAttribute("errorMessage", "Please select an image to upload.");
            return "redirect:/profile";
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            redirectAttrs.addFlashAttribute("errorMessage", "Image must be smaller than 2 MB.");
            return "redirect:/profile";
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            redirectAttrs.addFlashAttribute("errorMessage", "Only JPEG, PNG, WebP, or GIF images are allowed.");
            return "redirect:/profile";
        }

        try {
            User user = authService.findByEmail(userDetails.getUsername());

            // Delete the old avatar file if one exists
            deleteAvatarFile(user.getProfileImageUrl());

            // Save the new file
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);
            String ext = getExtension(file.getOriginalFilename(), contentType);
            String filename = UUID.randomUUID() + ext;
            Files.copy(file.getInputStream(), uploadPath.resolve(filename));

            String imageUrl = "/uploads/avatars/" + filename;
            authService.updateProfileImage(user.getId(), imageUrl);

            redirectAttrs.addFlashAttribute("successMessage", "Profile photo updated.");
        } catch (IOException e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Upload failed. Please try again.");
        }

        return "redirect:/profile";
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
        } catch (IOException e) {
            System.err.println("Warning: could not delete old avatar: " + imageUrl + " — " + e.getMessage());
        }
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
