package com.chickenexpress.foodorder.controller.admin;

import com.chickenexpress.foodorder.entity.Category;
import com.chickenexpress.foodorder.repository.CategoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Admin CRUD for product categories.
 * Supports image upload to uploads/categories/.
 * Old images are deleted when replaced or when the category is deleted.
 */
@Controller
@RequestMapping("/admin/categories")
public class CategoryManagementController {

    private static final String UPLOAD_DIR = "uploads/categories/";

    private final CategoryRepository categoryRepository;

    public CategoryManagementController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // ── List ─────────────────────────────────────────────────────────────────

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories",
            categoryRepository.findAll(
                org.springframework.data.domain.Sort.by("sortOrder", "name")));
        return "admin/category_management";
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @PostMapping("/new")
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(defaultValue = "0") int sortOrder,
                         @RequestParam("imageFile") MultipartFile imageFile,
                         RedirectAttributes ra) {
        if (name == null || name.isBlank()) {
            ra.addFlashAttribute("toastError", "Category name is required.");
            return "redirect:/admin/categories";
        }
        Category cat = new Category();
        cat.setName(name.trim());
        cat.setDescription(description != null ? description.trim() : null);
        cat.setSortOrder(sortOrder);
        cat.setActive(true);

        if (!imageFile.isEmpty()) {
            cat.setImageUrl(saveImage(imageFile));
        }

        categoryRepository.save(cat);
        ra.addFlashAttribute("toastSuccess", "Category \"" + cat.getName() + "\" created.");
        return "redirect:/admin/categories";
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(defaultValue = "0") int sortOrder,
                         @RequestParam(defaultValue = "false") boolean active,
                         @RequestParam("imageFile") MultipartFile imageFile,
                         RedirectAttributes ra) {
        Category cat = categoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));

        if (name == null || name.isBlank()) {
            ra.addFlashAttribute("toastError", "Category name is required.");
            return "redirect:/admin/categories";
        }
        cat.setName(name.trim());
        cat.setDescription(description != null ? description.trim() : null);
        cat.setSortOrder(sortOrder);
        cat.setActive(active);

        if (!imageFile.isEmpty()) {
            // Delete old image before saving new one
            deleteImageFile(cat.getImageUrl());
            cat.setImageUrl(saveImage(imageFile));
        }

        categoryRepository.save(cat);
        ra.addFlashAttribute("toastSuccess", "Category \"" + cat.getName() + "\" updated.");
        return "redirect:/admin/categories";
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        Category cat = categoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));

        if (!cat.getProducts().isEmpty()) {
            ra.addFlashAttribute("toastError",
                "Cannot delete \"" + cat.getName() + "\" — it has " +
                cat.getProducts().size() + " product(s). Reassign or delete them first.");
            return "redirect:/admin/categories";
        }
        String name = cat.getName();
        deleteImageFile(cat.getImageUrl());
        categoryRepository.delete(cat);
        ra.addFlashAttribute("toastSuccess", "Category \"" + name + "\" deleted.");
        return "redirect:/admin/categories";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String saveImage(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), uploadPath.resolve(filename));
            return "/uploads/categories/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save category image.", e);
        }
    }

    private void deleteImageFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            String relativePath = imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;
            Path filePath = Paths.get(relativePath).normalize();
            Path uploadRoot = Paths.get(UPLOAD_DIR).normalize().toAbsolutePath();
            if (!filePath.toAbsolutePath().startsWith(uploadRoot)) return;
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Warning: could not delete category image: " + imageUrl + " — " + e.getMessage());
        }
    }
}
