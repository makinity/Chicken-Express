package com.chickenexpress.foodorder.payment;

import com.chickenexpress.foodorder.entity.Order;
import com.chickenexpress.foodorder.entity.OrderItem;
import com.chickenexpress.foodorder.entity.Payment;
import com.chickenexpress.foodorder.exception.PaymentException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * HTTP client wrapper for the PayMongo REST API.
 *
 * Uses Java 11+ HttpClient — no extra dependency required.
 *
 * All monetary amounts sent to PayMongo are in centavos (PHP × 100).
 * Example: ₱150.00 → 15000 centavos
 *
 * Reference: https://developers.paymongo.com/reference/checkout-session-resource
 */
@Component
public class PayMongoClient {

    private static final Logger log = LoggerFactory.getLogger(PayMongoClient.class);

    @Value("${paymongo.secret-key}")
    private String secretKey;

    @Value("${paymongo.base-url}")
    private String baseUrl;

    @Value("${paymongo.success-url}")
    private String successUrl;

    @Value("${paymongo.cancel-url}")
    private String cancelUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Create Checkout Session ──────────────────────────────────────────────

    /**
     * Call POST /v1/checkout_sessions on the PayMongo API.
     *
     * Builds the request body from the order's line items, sends it, and
     * returns the session ID and hosted checkout URL.
     *
     * @param order   the confirmed order
     * @param payment the PENDING payment record (used for the session description)
     * @return a record containing the session ID and checkout URL
     * @throws PaymentException if the API call fails or returns a non-2xx response
     */
    public CheckoutSessionResult createCheckoutSession(Order order, Payment payment) {
        try {
            log.info("[PayMongo] Creating checkout session for order={} amount={}",
                order.getOrderNumber(), order.getTotalAmount());

            // Warn early if secret key looks empty
            if (secretKey == null || secretKey.isBlank()) {
                log.error("[PayMongo] PAYMONGO_SECRET_KEY is not set! Check your environment variables.");
                throw new PaymentException("PayMongo secret key is not configured. Set the PAYMONGO_SECRET_KEY environment variable.");
            }

            String requestBody = buildCheckoutSessionBody(order);
            log.debug("[PayMongo] Request body: {}", requestBody);

            String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/checkout_sessions"))
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            log.info("[PayMongo] Sending POST to {}", baseUrl + "/checkout_sessions");
            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("[PayMongo] Response status: {}", response.statusCode());
            log.debug("[PayMongo] Response body: {}", response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("[PayMongo] API error {} — Response: {}", response.statusCode(), response.body());
                throw new PaymentException(
                    "PayMongo API error " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String sessionId = root.at("/data/id").asText();
            String checkoutUrl = root.at("/data/attributes/checkout_url").asText();

            log.info("[PayMongo] Checkout session created. sessionId={} url={}", sessionId, checkoutUrl);
            return new CheckoutSessionResult(sessionId, checkoutUrl);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[PayMongo] Network error while creating checkout session", e);
            throw new PaymentException("Failed to create PayMongo checkout session: " + e.getMessage());
        }
    }

    // ── Request Body Builder ─────────────────────────────────────────────────

    private String buildCheckoutSessionBody(Order order) throws IOException {
        List<Map<String, Object>> lineItems = order.getOrderItems().stream()
            .map(this::toLineItem)
            .toList();

        Map<String, Object> attributes = Map.of(
            "billing", Map.of("name", order.getUser().getFullName(),
                              "email", order.getUser().getEmail()),
            "line_items", lineItems,
            "payment_method_types", List.of("card", "gcash", "grab_pay", "paymaya"),
            "success_url", successUrl + "?orderId=" + order.getId(),
            "cancel_url", cancelUrl,
            "description", "ChickenExpress Order " + order.getOrderNumber(),
            "reference_number", order.getOrderNumber(),
            "send_email_receipt", false,
            "show_description", true,
            "show_line_items", true
        );

        Map<String, Object> body = Map.of(
            "data", Map.of("attributes", attributes)
        );

        return objectMapper.writeValueAsString(body);
    }

    private Map<String, Object> toLineItem(OrderItem item) {
        // PayMongo expects amounts in centavos (integer)
        long amountInCentavos = item.getUnitPrice()
            .multiply(BigDecimal.valueOf(100))
            .longValue();

        return Map.of(
            "currency", "PHP",
            "amount", amountInCentavos,
            "name", item.getProduct().getName(),
            "quantity", item.getQuantity()
        );
    }

    // ── Result Record ────────────────────────────────────────────────────────

    /**
     * Holds the result from a successful checkout session creation call.
     *
     * @param sessionId   PayMongo checkout session ID (cs_xxxxxxxx)
     * @param checkoutUrl URL to redirect the customer to for payment
     */
    public record CheckoutSessionResult(String sessionId, String checkoutUrl) {}
}
