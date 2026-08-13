package com.chickenexpress.foodorder.controller.customer;

import com.chickenexpress.foodorder.entity.Category;
import com.chickenexpress.foodorder.entity.Product;
import com.chickenexpress.foodorder.repository.CategoryRepository;
import com.chickenexpress.foodorder.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Serves the public menu page.
 * No authentication required — menu is viewable by guests.
 */
@Controller
public class MenuController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public MenuController(ProductRepository productRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/menu")
    public String menuPage(@RequestParam(required = false) String search,
                           @RequestParam(required = false) Long categoryId,
                           Model model) {
        List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        List<Product> products;

        if (search != null && !search.isBlank()) {
            products = productRepository.searchByKeyword(search.trim());
            model.addAttribute("search", search.trim());
        } else if (categoryId != null) {
            products = productRepository.findByCategoryIdAndAvailableTrueOrderByNameAsc(categoryId);
            model.addAttribute("activeCategoryId", categoryId);
        } else {
            products = productRepository.findAllAvailableOrderedByCategoryAndName();
        }

        model.addAttribute("categories", categories);
        model.addAttribute("products", products);
        model.addAttribute("popularProducts", productRepository.findByPopularTrueAndAvailableTrue());
        return "customer/menu";
    }
}
