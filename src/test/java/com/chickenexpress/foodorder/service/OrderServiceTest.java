package com.chickenexpress.foodorder.service;

import com.chickenexpress.foodorder.entity.*;
import com.chickenexpress.foodorder.repository.OrderRepository;
import com.chickenexpress.foodorder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrderService.
 * Uses Mockito — no Spring context or DB required.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository  userRepository;
    @Mock private CartService     cartService;

    private OrderService orderService;

    private User testUser;
    private Product testProduct;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, userRepository, cartService);

        Category category = new Category("Chicken Meals");
        category.setId(1L);

        testProduct = new Product("Fried Chicken Solo", new BigDecimal("99.00"), category);
        testProduct.setId(1L);

        testUser = new User("Juan", "juan@example.com", "hashedpw");
        testUser.setId(1L);

        cartItem = new CartItem(testUser, testProduct, 2);
        cartItem.setId(1L);
    }

    // ── placeOrder ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("placeOrder: creates order from cart items with correct total")
    void placeOrder_createsOrder_withCorrectTotal() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCartItems(1L)).thenReturn(List.of(cartItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.placeOrder(1L, Order.OrderType.TAKEOUT, null, null);

        assertThat(order.getUser()).isEqualTo(testUser);
        assertThat(order.getOrderItems()).hasSize(1);
        // 2 × ₱99.00 = ₱198.00
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("198.00"));
        assertThat(order.getStatus()).isEqualTo(Order.Status.PENDING);
        assertThat(order.getOrderNumber()).startsWith("CE-");
    }

    @Test
    @DisplayName("placeOrder: throws IllegalStateException when cart is empty")
    void placeOrder_throws_whenCartEmpty() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCartItems(1L)).thenReturn(List.of());

        assertThatThrownBy(() ->
            orderService.placeOrder(1L, Order.OrderType.TAKEOUT, null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("empty cart");
    }

    // ── updateStatus ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus: changes order status to new value")
    void updateStatus_changesStatus() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(Order.Status.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateStatus(1L, Order.Status.PREPARING);

        assertThat(updated.getStatus()).isEqualTo(Order.Status.PREPARING);
    }

    @Test
    @DisplayName("updateStatus: throws IllegalArgumentException when order not found")
    void updateStatus_throws_whenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateStatus(99L, Order.Status.PREPARING))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }
}
