package com.chickenexpress.foodorder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures the STOMP-over-WebSocket message broker.
 *
 * <p>Connection flow:</p>
 * <ol>
 *   <li>Client connects to {@code /ws} (with SockJS fallback)</li>
 *   <li>Client subscribes to {@code /topic/admin} (admin broadcasts)
 *       or {@code /topic/user/{userId}} (per-user customer channel)</li>
 *   <li>Server calls {@link NotificationService} which uses
 *       {@code SimpMessagingTemplate} to push payloads to those topics</li>
 * </ol>
 *
 * <p>Topic map:</p>
 * <pre>
 *   /topic/admin          — broadcast to all connected admins
 *   /topic/user/{userId}  — private channel per customer
 * </pre>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Register the STOMP endpoint the browser connects to.
     * SockJS is enabled so the connection gracefully falls back to
     * HTTP long-polling on networks that block WebSockets.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Configure the in-memory STOMP message broker.
     * <ul>
     *   <li>{@code /topic} — broker destination prefix (server → clients)</li>
     *   <li>{@code /app}   — application destination prefix (clients → server,
     *                        if we ever add client-to-server messaging)</li>
     * </ul>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
