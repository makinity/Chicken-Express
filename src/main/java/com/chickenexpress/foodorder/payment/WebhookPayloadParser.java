package com.chickenexpress.foodorder.payment;

import com.chickenexpress.foodorder.exception.PaymentException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Parses and verifies incoming PayMongo webhook payloads.
 *
 * PayMongo signs each webhook request with an HMAC-SHA256 signature using
 * your webhook secret. We must verify this before trusting the payload.
 *
 * Signature verification reference:
 * https://developers.paymongo.com/docs/webhook-signature-verification
 *
 * The Paymongo-Signature header format:
 *   t=<timestamp>,te=<test_sig>,li=<live_sig>
 * We reconstruct the signed payload as "<timestamp>.<rawBody>" and compare
 * HMAC-SHA256 against the appropriate signature (test or live).
 */
@Component
public class WebhookPayloadParser {

    @Value("${paymongo.webhook-secret}")
    private String webhookSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Signature Verification ───────────────────────────────────────────────

    /**
     * Verify the PayMongo webhook signature.
     *
     * @param rawBody   the raw JSON body (must not be parsed before verification)
     * @param signature the value of the Paymongo-Signature header
     * @throws PaymentException if the signature is invalid or verification fails
     */
    public void verifySignature(String rawBody, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new PaymentException("Webhook secret is not configured.");
        }

        // Parse the signature header components
        String timestamp = null;
        String testSig = null;
        String liveSig = null;

        for (String part : signature.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            switch (kv[0].trim()) {
                case "t"  -> timestamp = kv[1].trim();
                case "te" -> testSig  = kv[1].trim();
                case "li" -> liveSig  = kv[1].trim();
            }
        }

        if (timestamp == null) {
            throw new PaymentException("Invalid signature header: missing timestamp.");
        }

        // Reconstruct the signed payload
        String signedPayload = timestamp + "." + rawBody;

        try {
            String expectedSig = computeHmacSha256(signedPayload, webhookSecret);

            // Accept either the test or live signature — whichever is present
            boolean valid = (testSig != null && MessageDigest.isEqual(
                                expectedSig.getBytes(StandardCharsets.UTF_8),
                                testSig.getBytes(StandardCharsets.UTF_8)))
                         || (liveSig != null && MessageDigest.isEqual(
                                expectedSig.getBytes(StandardCharsets.UTF_8),
                                liveSig.getBytes(StandardCharsets.UTF_8)));

            if (!valid) {
                throw new PaymentException("Webhook signature verification failed.");
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new PaymentException("Signature computation error: " + e.getMessage());
        }
    }

    // ── Payload Parsing ──────────────────────────────────────────────────────

    /**
     * Parse the raw JSON body into a WebhookEvent.
     *
     * @param rawBody the raw JSON webhook body
     * @return parsed WebhookEvent
     * @throws PaymentException if parsing fails
     */
    public WebhookEvent parse(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String eventType = root.at("/data/attributes/type").asText();
            JsonNode data = root.at("/data/attributes/data");

            String sessionId = data.at("/attributes/checkout_session_id").asText(null);
            if (sessionId == null) {
                // Some events nest session ID differently
                sessionId = data.at("/id").asText(null);
            }

            String paymentId = data.at("/id").asText(null);
            String paymentMethod = data.at("/attributes/source/type").asText(null);

            return new WebhookEvent(eventType, sessionId, paymentId, paymentMethod);

        } catch (Exception e) {
            throw new PaymentException("Failed to parse webhook payload: " + e.getMessage());
        }
    }

    // ── HMAC Helper ──────────────────────────────────────────────────────────

    private String computeHmacSha256(String data, String key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
            key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    // ── Result Record ────────────────────────────────────────────────────────

    /**
     * Parsed data from a PayMongo webhook event.
     *
     * @param eventType     e.g., "checkout_session.payment.paid"
     * @param sessionId     PayMongo checkout session ID
     * @param paymentId     PayMongo payment ID (null if event is not payment-related)
     * @param paymentMethod e.g., "card", "gcash", "grab_pay"
     */
    public record WebhookEvent(
        String eventType,
        String sessionId,
        String paymentId,
        String paymentMethod
    ) {}
}
