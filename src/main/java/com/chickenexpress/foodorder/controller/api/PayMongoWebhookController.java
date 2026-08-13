package com.chickenexpress.foodorder.controller.api;

import com.chickenexpress.foodorder.exception.PaymentException;
import com.chickenexpress.foodorder.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives PayMongo webhook events.
 *
 * Endpoint: POST /webhooks/paymongo
 *
 * This endpoint is intentionally excluded from:
 * - Authentication (PayMongo is a machine caller, not a browser user)
 * - CSRF protection (see SecurityConfig — CSRF disabled for this path)
 *
 * Security is instead enforced by verifying PayMongo's signature header
 * inside PaymentService → WebhookPayloadParser.
 *
 * For local development, use ngrok or a similar tunnel so PayMongo can
 * reach this endpoint:
 *   ngrok http 8080
 *   Then set the tunnel URL as your webhook URL in the PayMongo dashboard.
 */
@RestController
@RequestMapping("/webhooks")
public class PayMongoWebhookController {

    private final PaymentService paymentService;

    public PayMongoWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/paymongo")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "Paymongo-Signature", required = false) String signature) {

        if (signature == null || signature.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Missing signature header.");
        }

        try {
            paymentService.handleWebhook(rawBody, signature);
            return ResponseEntity.ok("Webhook processed.");
        } catch (PaymentException e) {
            // Return 400 so PayMongo does not retry indefinitely on a known bad payload.
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Return 500 so PayMongo retries — could be a transient DB issue.
            return ResponseEntity.internalServerError().body("Internal error.");
        }
    }
}
