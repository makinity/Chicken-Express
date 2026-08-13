package com.chickenexpress.foodorder.controller.admin;

import com.chickenexpress.foodorder.entity.Order;
import com.chickenexpress.foodorder.service.OrderService;
import com.chickenexpress.foodorder.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin order board: view all orders, update statuses, mark as paid manually.
 */
@Controller
@RequestMapping("/admin/orders")
public class OrderManagementController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public OrderManagementController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @GetMapping
    public String listOrders(@RequestParam(required = false) String status, Model model) {
        if (status != null && !status.isBlank()) {
            try {
                Order.Status orderStatus = Order.Status.valueOf(status.toUpperCase());
                model.addAttribute("orders", orderService.getOrdersByStatus(orderStatus));
                model.addAttribute("filterStatus", orderStatus);
            } catch (IllegalArgumentException ignored) {
                model.addAttribute("orders", orderService.getAllOrders());
            }
        } else {
            model.addAttribute("orders", orderService.getAllOrders());
        }
        model.addAttribute("statuses", Order.Status.values());
        return "admin/order_management";
    }

    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Long orderId, Model model) {
        model.addAttribute("order", orderService.findById(orderId));
        model.addAttribute("statuses", Order.Status.values());
        return "admin/order_detail";
    }

    @PostMapping("/{orderId}/status")
    public String updateStatus(@PathVariable Long orderId,
                               @RequestParam String status,
                               RedirectAttributes redirectAttributes) {
        try {
            Order.Status newStatus = Order.Status.valueOf(status.toUpperCase());
            orderService.updateStatus(orderId, newStatus);
            redirectAttributes.addFlashAttribute("message", "Order status updated to " + newStatus + ".");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid status: " + status);
        }
        return "redirect:/admin/orders/" + orderId;
    }

    @PostMapping("/{orderId}/mark-paid")
    public String markAsPaid(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        paymentService.markAsPaid(orderId);
        redirectAttributes.addFlashAttribute("message", "Order marked as paid.");
        return "redirect:/admin/orders/" + orderId;
    }
}
