package com.livegroupbuy.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.livegroupbuy.backend.live.LiveChatWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LiveChatWebSocketHandler liveChatHandler;

    public WebSocketConfig(LiveChatWebSocketHandler liveChatHandler) {
        this.liveChatHandler = liveChatHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveChatHandler, "/ws/live/{broadcastId}")
                .setAllowedOriginPatterns("*");
    }
}
