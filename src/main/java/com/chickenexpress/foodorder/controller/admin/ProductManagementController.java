package com.chickenexpress.foodorder.controller.admin;

import com.chickenexpress.foodorder.entity.Category;
import com.chickenexpress.foodorder.entity.Product;
import com.chickenexpress.foodorder.repository.CategoryRepository;
import com.chickenexpress.foodorder.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * CRUD operations for menu products.
 * Add / Edit actions are submitted from Bootstrap modals on product_management.html.
 * All POST handlers redirect back to the list with a toastSuccess / toastError flash.
 */
@Controller
@RequestMapping("/admin/products")
public class ProductManagementController {

    private static final String UPLOAD_DIR = "uploads/products/";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductManagementController(ProductRepository productRepository,
                                        CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    // ── List ─────────────────────────────────────────────────────────────────

    @GetMapping
    public String listProducts(Model model) {
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/product_management";
    }

    // ── Create (modal POST) ───────────────────────────────────────────────────

    @PostMapping("/new")
    public String createProduct(@RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam BigDecimal price,
                                @RequestParam Long categoryId,
                                @RequestParam(defaultValue = "false") boolean available,
                                @RequestParam(defaultValue = "false") boolean popular,
                                @RequestParam(defaultValue = "false") boolean spicy,
                                @RequestParam("imageFile") MultipartFile imageFile,
                                RedirectAttributes ra) {

        if (name == null || name.isBlank() || price == null || categoryId == null) {
            ra.addFlashAttribute("toastError", "Name, price and category are required.");
            return "redirect:/admin/products";
        }

        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Product product = new Product(name.trim(), price, category);
        product.setDescription(description != null ? description.trim() : null);
        product.setAvailable(available);
        product.setPopular(popular);
        product.setSpicy(spicy);

        if (!imageFile.isEmpty()) {
            product.setImageUrl(saveImage(imageFile));
        }

        productRepository.save(product);
        ra.addFlashAttribute("toastSuccess", "Product \"" + product.getName() + "\" created.");
        return "redirect:/admin/products";
    }

    // ── Update (modal POST) ───────────────────────────────────────────────────

    @PostMapping("/{id}/edit")
    public String updateProduct(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam BigDecimal price,
                                @RequestParam Long categoryId,
                                @RequestParam(defaultValue = "false") boolean available,
                                @RequestParam(defaultValue = "false") boolean popular,
                                @RequestParam(defaultValue = "false") boolean spicy,
                                @RequestParam("imageFile") MultipartFile imageFile,
                                RedirectAttributes ra) {

        Product product = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        product.setName(name.trim());
        product.setDescription(description != null ? description.trim() : null);
        product.setPrice(price);
        product.setCategory(category);
        product.setAvailable(available);
        product.setPopular(popular);
        product.setSpicy(spicy);

        if (!imageFile.isEmpty()) {
            // Delete the old image file before saving the new one
            deleteImageFile(product.getImageUrl());
            product.setImageUrl(saveImage(imageFile));
        }

        productRepository.save(product);
        ra.addFlashAttribute("toastSuccess", "Product \"" + product.getName() + "\" updated.");
        return "redirect:/admin/products";
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes ra) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        String name = product.getName();
        // Delete the image file from disk before removing the DB record
        deleteImageFile(product.getImageUrl());
        productRepository.delete(product);
        ra.addFlashAttribute("toastSuccess", "Product \"" + name + "\" deleted.");
        return "redirect:/admin/products";
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private String saveImage(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), uploadPath.resolve(filename));
            return "/uploads/products/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save product image.", e);
        }
    }

    /**
     * Deletes a product image file from disk.
     * Safe to call with null or blank URLs — silently skips.
     * Only deletes files that live inside the uploads/products/ directory.
     */
    private void deleteImageFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            // imageUrl is like "/uploads/products/uuid_filename.jpg"
            // Strip the leading slash and resolve against working directory
            String relativePath = imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;
            Path filePath = Paths.get(relativePath).normalize();

            // Safety check: only delete files inside the uploads/products directory
            Path uploadRoot = Paths.get(UPLOAD_DIR).normalize().toAbsolutePath();
            if (!filePath.toAbsolutePath().startsWith(uploadRoot)) {
                return;
            }

            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't fail — image cleanup is best-effort
            System.err.println("Warning: could not delete image file: " + imageUrl + " — " + e.getMessage());
        }
    }

}
