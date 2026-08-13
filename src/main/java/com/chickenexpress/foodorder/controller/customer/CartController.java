package com.chickenexpress.foodorder.controller.customer;

import com.chickenexpress.foodorder.entity.User;
import com.chickenexpress.foodorder.service.AuthService;
import com.chickenexpress.foodorder.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the customer's shopping cart page and all cart mutation actions.
 * All endpoints require authentication (enforced by SecurityConfig).
 */
@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final AuthService authService;

    public CartController(CartService cartService, AuthService authService) {
        this.cartService = cartService;
        this.authService = authService;
    }

    // ── View Cart ────────────────────────────────────────────────────────────

    @GetMapping
    public String cartPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = authService.findByEmail(userDetails.getUsername());
        model.addAttribute("cartItems", cartService.getCartItems(user.getId()));
        model.addAttribute("cartTotal", cartService.getCartTotal(user.getId()));
        return "customer/cart";
    }

    // ── Add Item ─────────────────────────────────────────────────────────────

    @PostMapping("/add")
    public String addItem(@AuthenticationPrincipal UserDetails userDetails,
                          @RequestParam Long productId,
                          @RequestParam(defaultValue = "1") int quantity,
                          RedirectAttributes redirectAttributes) {
        User user = authService.findByEmail(userDetails.getUsername());
        try {
            cartService.addToCart(user.getId(), productId, quantity);
            redirectAttributes.addFlashAttribute("message", "Item added to cart.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/menu";
    }

    // ── Update Quantity ──────────────────────────────────────────────────────

    @PostMapping("/update")
    public String updateQuantity(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam Long productId,
                                 @RequestParam int quantity) {
        User user = authService.findByEmail(userDetails.getUsername());
        cartService.updateQuantity(user.getId(), productId, quantity);
        return "redirect:/cart";
    }

    // ── Remove Item ──────────────────────────────────────────────────────────

    @PostMapping("/remove")
    public String removeItem(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam Long productId) {
        User user = authService.findByEmail(userDetails.getUsername());
        cartService.removeFromCart(user.getId(), productId);
        return "redirect:/cart";
    }
}
