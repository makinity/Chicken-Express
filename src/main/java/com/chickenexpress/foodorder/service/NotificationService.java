package com.chickenexpress.foodorder.service;

import com.chickenexpress.foodorder.dto.NotificationPayload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Sends real-time WebSocket notifications to admin and customer clients.
 *
 * <p>Uses Spring's {@link SimpMessagingTemplate} to push JSON payloads
 * to STOMP topic destinations:</p>
 * <ul>
 *   <li>{@code /topic/admin}         — broadcast to all connected admins</li>
 *   <li>{@code /topic/user/{userId}} — private channel per customer</li>
 * </ul>
 *
 * <p>All send calls are fire-and-forget — if no client is connected the
 * message is silently dropped (no persistence).</p>
 */
@Service
public class NotificationService {

    private static final String ADMIN_TOPIC = "/topic/admin";
    private static final String USER_TOPIC  = "/topic/user/";

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    /**
     * Push a notification to all connected admin sessions.
     */
    public void sendToAdmin(String type, String title, String message,
                             String link, String icon) {
        messagingTemplate.convertAndSend(
                ADMIN_TOPIC,
                NotificationPayload.of(type, title, message, link, icon)
        );
    }

    // ── Customer ──────────────────────────────────────────────────────────────

    /**
     * Push a notification to a specific customer's private channel.
     *
     * @param userId  the customer's database ID
     */
    public void sendToUser(Long userId, String type, String title,
                            String message, String link, String icon) {
        messagingTemplate.convertAndSend(
                USER_TOPIC + userId,
                NotificationPayload.of(type, title, message, link, icon)
        );
    }

    // ── Convenience named-event helpers ──────────────────────────────────────

    // A1 — new order placed (admin)
    public void notifyNewOrder(Long orderId, String orderNumber,
                                String customerName, String total) {
        sendToAdmin("NEW_ORDER", "New Order",
                orderNumber + " by " + customerName + " — " + total,
                "/admin/orders/" + orderId, "bi-receipt");
    }

    // A2 — payment confirmed (admin)
    public void notifyAdminPaymentConfirmed(Long orderId, String orderNumber,
                                             String method, String amount) {
        sendToAdmin("PAYMENT_CONFIRMED", "Payment Received",
                orderNumber + " paid via " + method + " — " + amount,
                "/admin/orders/" + orderId, "bi-credit-card");
    }

    // A3 — payment failed (admin)
    public void notifyAdminPaymentFailed(Long orderId, String orderNumber) {
        sendToAdmin("PAYMENT_FAILED", "Payment Failed",
                orderNumber + " payment failed — order cancelled",
                "/admin/orders/" + orderId, "bi-x-circle");
    }

    // A4 — manual mark as paid (admin)
    public void notifyAdminManualPaid(Long orderId, String orderNumber) {
        sendToAdmin("MANUAL_PAID", "Marked as Paid",
                orderNumber + " manually marked as paid",
                "/admin/orders/" + orderId, "bi-check-circle");
    }

    // A5 — new customer registered (admin)
    public void notifyNewCustomer(String fullName, String email) {
        sendToAdmin("NEW_CUSTOMER", "New Customer",
                fullName + " (" + email + ") just registered",
                "/admin/users", "bi-person-plus");
    }

    // C1 — order confirmed (customer)
    public void notifyOrderConfirmed(Long userId, Long orderId, String orderNumber) {
        sendToUser(userId, "ORDER_CONFIRMED", "Order Confirmed",
                "Your order " + orderNumber + " has been received!",
                "/orders/" + orderId, "bi-bag-check");
    }

    // C2 — payment confirmed (customer)
    public void notifyCustomerPaymentConfirmed(Long userId, Long orderId, String orderNumber) {
        sendToUser(userId, "PAYMENT_CONFIRMED", "Payment Received",
                "Payment for " + orderNumber + " confirmed. We\u2019re preparing it now!",
                "/orders/" + orderId, "bi-credit-card");
    }

    // C3 — payment failed (customer)
    public void notifyCustomerPaymentFailed(Long userId, Long orderId, String orderNumber) {
        sendToUser(userId, "PAYMENT_FAILED", "Payment Failed",
                "Your payment for " + orderNumber + " failed. Please try again.",
                "/orders/" + orderId, "bi-x-circle");
    }

    // C4 — order preparing (customer)
    public void notifyOrderPreparing(Long userId, Long orderId, String orderNumber) {
        sendToUser(userId, "ORDER_PREPARING", "Being Prepared",
                "Your order " + orderNumber + " is now being prepared!",
                "/orders/" + orderId, "bi-fire");
    }

    // C5 — order ready (customer)
    public void notifyOrderReady(Long userId, Long orderId, String orderNumber) {
        sendToUser(userId, "ORDER_READY", "\uD83C\uDF57 Order Ready!",
                "Your order " + orderNumber + " is ready for pickup!",
                "/orders/" + orderId, "bi-bag-check-fill");
    }

    // C6 — order completed (customer)
    public void notifyOrderCompleted(Long userId, Long orderId, String orderNumber) {
        sendToUser(userId, "ORDER_COMPLETED", "Order Completed",
                "Thank you! " + orderNumber + " has been completed. Enjoy your meal!",
                "/orders/" + orderId, "bi-emoji-smile");
    }

    // C7 — order cancelled (customer)
    public void notifyOrderCancelled(Long userId, Long orderId, String orderNumber) {
        sendToUser(userId, "ORDER_CANCELLED", "Order Cancelled",
                "Your order " + orderNumber + " has been cancelled.",
                "/orders/" + orderId, "bi-x-circle");
    }
}
