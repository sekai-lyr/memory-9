package com.sekai.sekai_form.config;

import com.sekai.sekai_form.websocket.Live2DWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class Live2DWebSocketConfig implements WebSocketConfigurer {
    private final Live2DWebSocketHandler handler;
    public Live2DWebSocketConfig(Live2DWebSocketHandler handler) { this.handler = handler; }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/live2d").setAllowedOrigins("*");
    }
}