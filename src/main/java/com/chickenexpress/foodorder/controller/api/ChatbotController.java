package com.chickenexpress.foodorder.controller.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Proxies chat messages to the Groq API.
 * POST /api/chat  { "message": "..." }
 * Returns         { "reply": "..." }
 */
@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

    @Value("${groq.api-key:}")
    private String apiKey;

    @Value("${groq.api-url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.model:llama3-8b-8192}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "You are a friendly and helpful assistant for ChickenExpress, " +
        "a chicken food ordering restaurant. Help customers with menu questions, " +
        "order information, pricing, promotions, and general inquiries. " +
        "Keep responses concise and friendly. If asked about something unrelated " +
        "to food or the restaurant, politely redirect the conversation back to " +
        "how you can help with their ChickenExpress experience.";

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String userMessage = body.get("message");

        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message cannot be empty."));
        }

        if (apiKey == null || apiKey.isBlank() || apiKey.equals("gsk_REPLACE_ME")) {
            return ResponseEntity.ok(Map.of("reply",
                "Hi! I'm the ChickenExpress assistant. " +
                "(Chatbot is not configured yet — please set GROQ_API_KEY.)"));
        }

        try {
            // Build request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 512);
            requestBody.put("temperature", 0.7);

            ArrayNode messages = requestBody.putArray("messages");

            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody), headers);

            // Call Groq
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String reply = root
                    .path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asText("Sorry, I could not get a response. Please try again.");

            return ResponseEntity.ok(Map.of("reply", reply));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("reply",
                "Sorry, I'm having trouble connecting right now. Please try again shortly."));
        }
    }
}
