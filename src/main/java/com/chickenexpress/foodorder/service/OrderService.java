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

    private static final String PESO = "\u20B1";

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final NotificationService notificationService;

    // In-memory counter for sequential order numbers per day.
    // Seeded from the DB on startup so restarts don't cause duplicates.
    private final AtomicLong dailyCounter = new AtomicLong(0);
    private volatile String counterDate = "";  // tracks which date the counter is for

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        CartService cartService,
                        NotificationService notificationService) {
        this.orderRepository     = orderRepository;
        this.userRepository      = userRepository;
        this.cartService         = cartService;
        this.notificationService = notificationService;
    }

    /** Seed the daily counter from today's existing orders so restarts don't produce duplicates. */
    @PostConstruct
    public void initDailyCounter() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        counterDate = today;

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long todayCount = orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        dailyCounter.set(todayCount);
    }

    // ── Place Order ──────────────────────────────────────────────────────────

    /**
     * Convert the user's cart into a confirmed Order.
     * Fires: A1 (admin — new order) + C1 (customer — order confirmed).
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

        cartService.clearCart(userId);

        // ── A1: notify admin of new order ──────────────────────────────────
        notificationService.notifyNewOrder(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                user.getFullName(),
                PESO + String.format("%,.2f", savedOrder.getTotalAmount())
        );

        // ── C1: confirm order to customer ──────────────────────────────────
        notificationService.notifyOrderConfirmed(
                userId, savedOrder.getId(), savedOrder.getOrderNumber()
        );

        return savedOrder;
    }

    // ── Status Updates ───────────────────────────────────────────────────────

    /**
     * Update order status (admin action).
     * Fires customer notifications C4–C7 based on the new status.
     */
    public Order updateStatus(Long orderId, Order.Status newStatus) {
        Order order = findById(orderId);
        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        Long userId   = saved.getUser().getId();
        Long id       = saved.getId();
        String number = saved.getOrderNumber();

        // Fire the appropriate customer notification
        switch (newStatus) {
            case PREPARING  -> notificationService.notifyOrderPreparing(userId, id, number);
            case READY      -> notificationService.notifyOrderReady(userId, id, number);
            case COMPLETED  -> notificationService.notifyOrderCompleted(userId, id, number);
            case CANCELLED  -> notificationService.notifyOrderCancelled(userId, id, number);
            default -> { /* PENDING — no customer notification needed */ }
        }

        return saved;
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

    private synchronized String generateOrderNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if (!today.equals(counterDate)) {
            counterDate = today;
            dailyCounter.set(0);
        }
        long seq = dailyCounter.incrementAndGet();
        return String.format("CE-%s-%04d", today, seq);
    }
}
