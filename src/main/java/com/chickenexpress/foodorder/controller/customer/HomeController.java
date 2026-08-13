package com.chickenexpress.foodorder.controller.customer;

import com.chickenexpress.foodorder.entity.Category;
import com.chickenexpress.foodorder.entity.Product;
import com.chickenexpress.foodorder.repository.CategoryRepository;
import com.chickenexpress.foodorder.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Serves the public landing / home page at /.
 * No authentication required — visible to all guests and users.
 */
@Controller
public class HomeController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public HomeController(ProductRepository productRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/")
    public String homePage(Model model) {
        List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        List<Product> popularProducts = productRepository.findByPopularTrueAndAvailableTrue();

        model.addAttribute("categories", categories);
        model.addAttribute("popularProducts", popularProducts);
        model.addAttribute("pageTitle", "ChickenExpress — Fresh, Crispy & Delicious");
        model.addAttribute("extraCss", "/css/home.css");
        return "customer/home";
    }
}
