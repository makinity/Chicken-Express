package com.chickenexpress.foodorder.service;

import com.chickenexpress.foodorder.entity.*;
import com.chickenexpress.foodorder.repository.OrderRepository;
import com.chickenexpress.foodorder.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles order lifecycle: placement, status updates, and history queries.
 *
 * Order flow:
 *   CartService provides items → OrderService creates Order + OrderItems →
 *   CartService clears cart → PaymentService initiates PayMongo checkout
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService;

    // In-memory counter for sequential order numbers per day.
    // Seeded from the DB on startup so restarts don't cause duplicates.
    private final AtomicLong dailyCounter = new AtomicLong(0);
    private volatile String counterDate = "";  // tracks which date the counter is for

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        CartService cartService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
    }

    /** Seed the daily counter from today's existing orders so restarts don't produce duplicates. */
    @PostConstruct
    public void initDailyCounter() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        counterDate = today;

        // Count how many orders already exist today — start the counter from there
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long todayCount = orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        dailyCounter.set(todayCount);
    }

    // ── Place Order ──────────────────────────────────────────────────────────

    /**
     * Convert the user's cart into a confirmed Order.
     *
     * Steps:
     * 1. Load cart items — throw if cart is empty
     * 2. Build Order + OrderItem records (snapshot unit prices)
     * 3. Save order
     * 4. Clear the cart
     * 5. Return the saved Order (caller then initiates payment)
     *
     * @param userId      the logged-in user
     * @param orderType   DINE_IN | TAKEOUT | DELIVERY
     * @param notes       optional special instructions
     * @param deliveryAddress required when orderType = DELIVERY
     */
    public Order placeOrder(Long userId, Order.OrderType orderType,
                            String notes, String deliveryAddress) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<CartItem> cartItems = cartService.getCartItems(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot place an order with an empty cart.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(Order.Status.PENDING);
        order.setOrderType(orderType);
        order.setNotes(notes);
        order.setDeliveryAddress(deliveryAddress);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            BigDecimal unitPrice = cartItem.getProduct().getPrice();
            OrderItem orderItem = new OrderItem(order, cartItem.getProduct(),
                                                cartItem.getQuantity(), unitPrice);
            order.getOrderItems().add(orderItem);
            total = total.add(orderItem.getSubtotal());
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        // Clear the cart now that the order is placed
        cartService.clearCart(userId);

        return savedOrder;
    }

    // ── Status Updates ───────────────────────────────────────────────────────

    /**
     * Update order status (admin action).
     * Validates the transition is logical before saving.
     */
    public Order updateStatus(Long orderId, Order.Status newStatus) {
        Order order = findById(orderId);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Order findById(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrderHistory(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(Order.Status status) {
        return orderRepository.findByStatusOrderByCreatedAtAsc(status);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Generates a human-readable order number in the format CE-YYYYMMDD-XXXX. */
    private synchronized String generateOrderNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Reset counter if the date has changed (midnight rollover)
        if (!today.equals(counterDate)) {
            counterDate = today;
            dailyCounter.set(0);
        }

        long seq = dailyCounter.incrementAndGet();
        return String.format("CE-%s-%04d", today, seq);
    }
}
