package com.chickenexpress.foodorder.controller.customer;

import com.chickenexpress.foodorder.entity.Order;
import com.chickenexpress.foodorder.entity.User;
import com.chickenexpress.foodorder.service.AuthService;
import com.chickenexpress.foodorder.service.OrderService;
import com.chickenexpress.foodorder.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles the checkout flow:
 *   1. GET /checkout  — display order summary before confirming
 *   2. POST /checkout — place the order and redirect to PayMongo
 *   3. GET /checkout/success — shown after successful PayMongo payment
 *   4. GET /checkout/cancel  — shown when customer cancels on PayMongo
 */
@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final AuthService authService;

    public CheckoutController(OrderService orderService,
                               PaymentService paymentService,
                               AuthService authService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.authService = authService;
    }

    // ── Review Checkout ──────────────────────────────────────────────────────

    @GetMapping
    public String checkoutPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = authService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        return "customer/checkout";
    }

    // ── Place Order + Redirect to PayMongo ───────────────────────────────────

    @PostMapping
    public String placeOrder(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam(defaultValue = "TAKEOUT") String orderType,
                             @RequestParam(required = false) String notes,
                             @RequestParam(required = false) String deliveryAddress,
                             @RequestParam(required = false) String contactPhone,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        User user = authService.findByEmail(userDetails.getUsername());

        // Server-side: address and phone required for delivery
        if ("DELIVERY".equals(orderType)) {
            if (deliveryAddress == null || deliveryAddress.isBlank()) {
                redirectAttributes.addFlashAttribute("error",
                        "A delivery address is required for delivery orders.");
                return "redirect:/checkout";
            }
            if (contactPhone == null || contactPhone.isBlank()) {
                redirectAttributes.addFlashAttribute("error",
                        "A contact phone number is required for delivery orders.");
                return "redirect:/checkout";
            }
        }

        try {
            Order order = orderService.placeOrder(
                user.getId(),
                Order.OrderType.valueOf(orderType),
                notes,
                deliveryAddress
            );

            // Build base URL dynamically from the incoming request
            // Works for both localhost:8080 and ngrok automatically
            String scheme   = request.getHeader("X-Forwarded-Proto") != null
                                ? request.getHeader("X-Forwarded-Proto")
                                : request.getScheme();
            String host     = request.getHeader("X-Forwarded-Host") != null
                                ? request.getHeader("X-Forwarded-Host")
                                : request.getServerName();
            int    port     = request.getServerPort();
            String baseUrl  = scheme + "://" + host +
                              (("http".equals(scheme) && port == 80) ||
                               ("https".equals(scheme) && port == 443) ||
                               request.getHeader("X-Forwarded-Host") != null
                                ? "" : ":" + port);

            String checkoutUrl = paymentService.initiateCheckout(order, baseUrl);
            return "redirect:" + checkoutUrl;

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }

    // ── Payment Result Pages ─────────────────────────────────────────────────

    @GetMapping("/success")
    public String successPage(@RequestParam(required = false) String orderId, Model model) {
        model.addAttribute("orderId", orderId);
        return "customer/checkout_success";
    }

    @GetMapping("/cancel")
    public String cancelPage() {
        return "customer/checkout_cancel";
    }
}
