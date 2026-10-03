package com.livegroupbuy.backend.live;

import java.io.IOException;
import java.util.Deque;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import tools.jackson.databind.ObjectMapper;

@Component
public class LiveChatWebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_HISTORY = 100;

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Deque<LiveChatMessage>> history = new ConcurrentHashMap<>();

    public LiveChatWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        Long broadcastId = broadcastId(session);
        rooms.computeIfAbsent(broadcastId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        for (LiveChatMessage message : history.computeIfAbsent(broadcastId, ignored -> new ConcurrentLinkedDeque<>())) {
            if (session.isOpen()) session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage payload) throws IOException {
        IncomingMessage incoming = objectMapper.readValue(payload.getPayload(), IncomingMessage.class);
        String user = clean(incoming.user(), 50);
        String messageText = clean(incoming.message(), 100);
        if (user == null || messageText == null) return;

        Long broadcastId = broadcastId(session);
        LiveChatMessage message = new LiveChatMessage(UUID.randomUUID().toString(), user, messageText, System.currentTimeMillis());
        Deque<LiveChatMessage> roomHistory = history.computeIfAbsent(broadcastId, ignored -> new ConcurrentLinkedDeque<>());
        roomHistory.addLast(message);
        while (roomHistory.size() > MAX_HISTORY) roomHistory.pollFirst();

        String json = objectMapper.writeValueAsString(message);
        for (WebSocketSession receiver : rooms.getOrDefault(broadcastId, Set.of())) {
            if (!receiver.isOpen()) continue;
            try {
                receiver.sendMessage(new TextMessage(json));
            } catch (IOException exception) {
                Set<WebSocketSession> sessions = rooms.get(broadcastId);
                if (sessions != null) sessions.remove(receiver);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long broadcastId = broadcastId(session);
        Set<WebSocketSession> sessions = rooms.get(broadcastId);
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) rooms.remove(broadcastId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }

    private Long broadcastId(WebSocketSession session) {
        String path = session.getUri() == null ? "" : session.getUri().getPath();
        String value = path.substring(path.lastIndexOf('/') + 1);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("올바르지 않은 방송 채팅 주소입니다.");
        }
    }

    private String clean(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String cleaned = value.trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }

    private record IncomingMessage(String user, String message) {
    }
}
