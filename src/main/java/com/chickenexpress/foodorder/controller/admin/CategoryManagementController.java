package com.chickenexpress.foodorder.controller.admin;

import com.chickenexpress.foodorder.entity.Category;
import com.chickenexpress.foodorder.repository.CategoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin CRUD for product categories.
 * All actions redirect back to /admin/categories with a flash message
 * that the toast JS picks up and displays.
 */
@Controller
@RequestMapping("/admin/categories")
public class CategoryManagementController {

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
        categoryRepository.delete(cat);
        ra.addFlashAttribute("toastSuccess", "Category \"" + name + "\" deleted.");
        return "redirect:/admin/categories";
    }
}
