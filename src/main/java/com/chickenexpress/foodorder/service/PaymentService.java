package com.chickenexpress.foodorder.service;

import com.chickenexpress.foodorder.entity.Order;
import com.chickenexpress.foodorder.entity.Payment;
import com.chickenexpress.foodorder.exception.PaymentException;
import com.chickenexpress.foodorder.payment.PayMongoClient;
import com.chickenexpress.foodorder.payment.WebhookPayloadParser;
import com.chickenexpress.foodorder.repository.OrderRepository;
import com.chickenexpress.foodorder.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates PayMongo payment operations.
 *
 * Responsibilities:
 * - Create a PayMongo checkout session when a customer proceeds to pay
 * - Handle incoming webhook events (payment succeeded / failed)
 * - Update Order and Payment status accordingly
 */
@Service
@Transactional
public class PaymentService {

    private final PayMongoClient payMongoClient;
    private final WebhookPayloadParser webhookPayloadParser;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(PayMongoClient payMongoClient,
                          WebhookPayloadParser webhookPayloadParser,
                          PaymentRepository paymentRepository,
                          OrderRepository orderRepository) {
        this.payMongoClient = payMongoClient;
        this.webhookPayloadParser = webhookPayloadParser;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    // ── Create Checkout Session ──────────────────────────────────────────────

    /**
     * Initiate a PayMongo hosted checkout session for the given order.
     *
     * 1. Creates a Payment record (PENDING)
     * 2. Calls PayMongo Checkout Session API via PayMongoClient
     * 3. Stores the returned session ID in the Payment record
     * 4. Returns the hosted checkout URL to redirect the customer to
     *
     * @param order the order to pay for
     * @return the PayMongo hosted checkout URL
     * @throws PaymentException if the API call fails
     */
    public String initiateCheckout(Order order) {
        // Create a PENDING payment record
        Payment payment = new Payment(order, order.getTotalAmount());
        paymentRepository.save(payment);

        // Call PayMongo — returns {sessionId, checkoutUrl}
        PayMongoClient.CheckoutSessionResult result =
            payMongoClient.createCheckoutSession(order, payment);

        // Store the session ID so we can match it when the webhook arrives
        payment.setPaymongoSessionId(result.sessionId());
        paymentRepository.save(payment);

        return result.checkoutUrl();
    }

    // ── Webhook Handling ─────────────────────────────────────────────────────

    /**
     * Process a raw PayMongo webhook payload.
     *
     * 1. Verify the PayMongo signature header
     * 2. Parse the event type and extract the session ID
     * 3. Look up the Payment record by session ID
     * 4. Update Payment.status and Order.status based on the event
     *
     * @param rawBody   the raw JSON body from the webhook POST
     * @param signature the value of the PayMongo-Signature header
     * @throws PaymentException if signature verification fails or event parsing fails
     */
    public void handleWebhook(String rawBody, String signature) {
        // Verify signature — throws PaymentException if invalid
        webhookPayloadParser.verifySignature(rawBody, signature);

        // Parse the event
        WebhookPayloadParser.WebhookEvent event = webhookPayloadParser.parse(rawBody);

        // Look up the payment record
        Payment payment = paymentRepository.findByPaymongoSessionId(event.sessionId())
            .orElseThrow(() -> new PaymentException(
                "No payment record found for session: " + event.sessionId()));

        payment.setWebhookEventType(event.eventType());

        switch (event.eventType()) {
            case "checkout_session.payment.paid" -> {
                payment.setStatus(Payment.Status.PAID);
                payment.setPaymongoPaymentId(event.paymentId());
                payment.setPaymentMethod(event.paymentMethod());
                payment.setPaidAt(java.time.LocalDateTime.now());
                payment.getOrder().setStatus(Order.Status.PREPARING);
            }
            case "checkout_session.payment.failed" -> {
                payment.setStatus(Payment.Status.FAILED);
                payment.getOrder().setStatus(Order.Status.CANCELLED);
            }
            default -> {
                // Unknown event type — log and ignore
            }
        }

        paymentRepository.save(payment);
        orderRepository.save(payment.getOrder());
    }

    // ── Admin: Manual Mark as Paid ───────────────────────────────────────────

    /**
     * Manually mark an order as paid (admin fallback for testing before
     * PayMongo is wired, or for cash/in-person transactions).
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
    }
}
