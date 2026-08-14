package com.chickenexpress.foodorder.service;

import com.chickenexpress.foodorder.entity.Order;
import com.chickenexpress.foodorder.entity.Payment;
import com.chickenexpress.foodorder.exception.PaymentException;
import com.chickenexpress.foodorder.payment.PayMongoClient;
import com.chickenexpress.foodorder.payment.WebhookPayloadParser;
import com.chickenexpress.foodorder.repository.OrderRepository;
import com.chickenexpress.foodorder.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates PayMongo payment operations.
 *
 * Responsibilities:
 * - Create a PayMongo checkout session when a customer proceeds to pay
 * - Handle incoming webhook events (payment succeeded / failed)
 * - Update Order and Payment status accordingly
 * - Fire WebSocket notifications (A2, A3, A4, C2, C3) via NotificationService
 */
@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String PESO = "\u20B1";

    private final PayMongoClient payMongoClient;
    private final WebhookPayloadParser webhookPayloadParser;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public PaymentService(PayMongoClient payMongoClient,
                          WebhookPayloadParser webhookPayloadParser,
                          PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          NotificationService notificationService) {
        this.payMongoClient       = payMongoClient;
        this.webhookPayloadParser = webhookPayloadParser;
        this.paymentRepository    = paymentRepository;
        this.orderRepository      = orderRepository;
        this.notificationService  = notificationService;
    }

    // ── Create Checkout Session ──────────────────────────────────────────────

    /**
     * Initiate a PayMongo hosted checkout session for the given order.
     *
     * @param order   the confirmed order
     * @param baseUrl the base URL of the current request (e.g. https://ngrok... or http://localhost:8080)
     */
    public String initiateCheckout(Order order, String baseUrl) {
        log.info("[Payment] Initiating checkout for order={} user={} amount={} baseUrl={}",
            order.getOrderNumber(), order.getUser().getEmail(), order.getTotalAmount(), baseUrl);

        Payment payment = new Payment(order, order.getTotalAmount());
        paymentRepository.save(payment);

        try {
            PayMongoClient.CheckoutSessionResult result =
                payMongoClient.createCheckoutSession(order, payment, baseUrl);

            payment.setPaymongoSessionId(result.sessionId());
            paymentRepository.save(payment);

            log.info("[Payment] Checkout session stored. sessionId={}", result.sessionId());
            return result.checkoutUrl();

        } catch (PaymentException e) {
            log.error("[Payment] Failed to create checkout session for order={}: {}",
                order.getOrderNumber(), e.getMessage(), e);
            throw e;
        }
    }

    // ── Webhook Handling ─────────────────────────────────────────────────────

    /**
     * Process a raw PayMongo webhook payload.
     * Fires A2/C2 on payment.paid, A3/C3 on payment.failed.
     */
    public void handleWebhook(String rawBody, String signature) {
        webhookPayloadParser.verifySignature(rawBody, signature);

        WebhookPayloadParser.WebhookEvent event = webhookPayloadParser.parse(rawBody);

        Payment payment = paymentRepository.findByPaymongoSessionId(event.sessionId())
            .orElseThrow(() -> new PaymentException(
                "No payment record found for session: " + event.sessionId()));

        payment.setWebhookEventType(event.eventType());

        Order order = payment.getOrder();

        switch (event.eventType()) {
            case "checkout_session.payment.paid" -> {
                payment.setStatus(Payment.Status.PAID);
                payment.setPaymongoPaymentId(event.paymentId());
                payment.setPaymentMethod(event.paymentMethod());
                payment.setPaidAt(java.time.LocalDateTime.now());
                order.setStatus(Order.Status.PREPARING);

                String method = event.paymentMethod() != null
                        ? event.paymentMethod() : "online";
                String amount = PESO + String.format("%,.2f", payment.getAmount());

                // A2 — admin
                notificationService.notifyAdminPaymentConfirmed(
                        order.getId(), order.getOrderNumber(), method, amount);
                // C2 — customer
                notificationService.notifyCustomerPaymentConfirmed(
                        order.getUser().getId(), order.getId(), order.getOrderNumber());
            }
            case "checkout_session.payment.failed" -> {
                payment.setStatus(Payment.Status.FAILED);
                order.setStatus(Order.Status.CANCELLED);

                // A3 — admin
                notificationService.notifyAdminPaymentFailed(
                        order.getId(), order.getOrderNumber());
                // C3 — customer
                notificationService.notifyCustomerPaymentFailed(
                        order.getUser().getId(), order.getId(), order.getOrderNumber());
            }
            default -> { /* unknown — log and ignore */ }
        }

        paymentRepository.save(payment);
        orderRepository.save(order);
    }

    // ── Admin: Manual Mark as Paid ───────────────────────────────────────────

    /**
     * Manually mark an order as paid.
     * Fires A4 (admin notification).
     */
    public void markAsPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseGet(() -> {
                Payment p = new Payment(order, order.getTotalAmount());
                return paymentRepository.save(p);
            });

        payment.setStatus(Payment.Status.PAID);
        payment.setPaidAt(java.time.LocalDateTime.now());
        paymentRepository.save(payment);

        order.setStatus(Order.Status.PREPARING);
        orderRepository.save(order);

        // A4 — admin
        notificationService.notifyAdminManualPaid(order.getId(), order.getOrderNumber());

        // C2 — also tell the customer their payment was confirmed
        notificationService.notifyCustomerPaymentConfirmed(
                order.getUser().getId(), order.getId(), order.getOrderNumber());
    }
}
