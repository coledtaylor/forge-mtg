package forge.web.starter.config;

import forge.web.starter.websocket.GameWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration.
 *
 * @Configuration - Marks this as a Spring configuration class
 * @EnableWebSocket - Enables WebSocket support
 * <p>
 * WebSockets allow real-time, bidirectional communication between
 * the server and client. Perfect for game state updates!
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler) {
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register WebSocket endpoint at /ws/game/{gameId}
        registry.addHandler(gameWebSocketHandler, "/ws/game/**")
            .setAllowedOrigins("*");  // In production, specify exact origins

        System.out.println("🔌 WebSocket endpoint registered at /ws/game/**");
    }
}

