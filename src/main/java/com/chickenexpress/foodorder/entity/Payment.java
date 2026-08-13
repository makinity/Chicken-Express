package com.chickenexpress.foodorder.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a payment attempt against an Order via PayMongo.
 *
 * One-to-one with Order. A new Payment record is created when a checkout
 * session is initiated; the status is updated when the PayMongo webhook arrives.
 *
 * Status values mirror PayMongo checkout session statuses:
 *   PENDING  — session created, customer not yet redirected or hasn't paid
 *   PAID     — webhook confirmed successful payment
 *   FAILED   — payment failed or expired
 *   REFUNDED — manually recorded after a refund is processed
 */
@Entity
@Table(name = "payments")
public class Payment {

    public enum Status {
        PENDING,
        PAID,
        FAILED,
        REFUNDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** PayMongo checkout session ID (cs_xxxxxxxx). */
    @Column(length = 100)
    private String paymongoSessionId;

    /** PayMongo payment intent / payment ID returned in the webhook. */
    @Column(length = 100)
    private String paymongoPaymentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** Currency code (e.g., "PHP"). */
    @Column(length = 10)
    private String currency = "PHP";

    /** Payment method used (e.g., "card", "gcash", "grab_pay", "paymaya"). */
    @Column(length = 50)
    private String paymentMethod;

    /** Raw webhook event type received from PayMongo (for audit/debugging). */
    @Column(length = 100)
    private String webhookEventType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    // ── Relationships ────────────────────────────────────────────────────────

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ── Constructors ─────────────────────────────────────────────────────────

    public Payment() {}

    public Payment(Order order, BigDecimal amount) {
        this.order = order;
        this.amount = amount;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPaymongoSessionId() { return paymongoSessionId; }
    public void setPaymongoSessionId(String paymongoSessionId) { this.paymongoSessionId = paymongoSessionId; }

    public String getPaymongoPaymentId() { return paymongoPaymentId; }
    public void setPaymongoPaymentId(String paymongoPaymentId) { this.paymongoPaymentId = paymongoPaymentId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getWebhookEventType() { return webhookEventType; }
    public void setWebhookEventType(String webhookEventType) { this.webhookEventType = webhookEventType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
}
