package com.chickenexpress.foodorder.controller.customer;

import com.chickenexpress.foodorder.entity.User;
import com.chickenexpress.foodorder.service.AuthService;
import com.chickenexpress.foodorder.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Displays the logged-in customer's order history and individual order details.
 */
@Controller
@RequestMapping("/orders")
public class OrderHistoryController {

    private final OrderService orderService;
    private final AuthService authService;

    public OrderHistoryController(OrderService orderService, AuthService authService) {
        this.orderService = orderService;
        this.authService = authService;
    }

    @GetMapping
    public String orderHistory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = authService.findByEmail(userDetails.getUsername());
        model.addAttribute("orders", orderService.getOrderHistory(user.getId()));
        return "customer/order_history";
    }

    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Long orderId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        User user = authService.findByEmail(userDetails.getUsername());
        var order = orderService.findById(orderId);

        // Prevent customers from viewing other users' orders
        if (!order.getUser().getId().equals(user.getId())) {
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        return "customer/order_detail";
    }
}
