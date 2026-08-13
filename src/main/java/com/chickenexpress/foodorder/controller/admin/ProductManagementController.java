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
        productRepository.delete(product);
        ra.addFlashAttribute("toastSuccess", "Product \"" + name + "\" deleted.");
        return "redirect:/admin/products";
    }

    // ── Helper ────────────────────────────────────────────────────────────────

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
}
