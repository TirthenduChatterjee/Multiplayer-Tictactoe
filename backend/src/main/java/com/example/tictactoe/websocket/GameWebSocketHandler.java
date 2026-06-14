package com.example.tictactoe.websocket;

import com.example.tictactoe.dto.RoomView;
import com.example.tictactoe.service.GameRuleException;
import com.example.tictactoe.service.GameService;
import com.example.tictactoe.service.RoomNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriTemplate;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final GameService gameService;
    private final ObjectMapper objectMapper;
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final UriTemplate roomTemplate = new UriTemplate("/ws/rooms/{code}");

    public GameWebSocketHandler(GameService gameService, ObjectMapper objectMapper) {
        this.gameService = gameService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String code = extractCode(session).toUpperCase();
        session.getAttributes().put("code", code);
        roomSessions.computeIfAbsent(code, key -> ConcurrentHashMap.newKeySet()).add(session);
        send(session, "ROOM_STATE", gameService.getRoom(code));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String code = (String) session.getAttributes().get("code");
        JsonNode payload = objectMapper.readTree(message.getPayload());
        String type = payload.path("type").asText();

        try {
            if ("MOVE".equals(type)) {
                RoomView state = gameService.makeMove(
                        code,
                        payload.path("cellIndex").asInt(),
                        payload.path("playerName").asText()
                );
                broadcast(code, "ROOM_STATE", state);
                return;
            }

            if ("RESTART".equals(type)) {
                RoomView state = gameService.restart(code, payload.path("playerName").asText());
                broadcast(code, "ROOM_STATE", state);
                return;
            }

            send(session, "ERROR", Map.of("message", "Unknown message type."));
        } catch (GameRuleException | RoomNotFoundException exception) {
            send(session, "ERROR", Map.of("message", exception.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String code = (String) session.getAttributes().get("code");
        if (code == null) {
            return;
        }
        Set<WebSocketSession> sessions = roomSessions.getOrDefault(code, Collections.emptySet());
        sessions.remove(session);
        if (sessions.isEmpty()) {
            roomSessions.remove(code);
        }
    }

    public void broadcastRoomState(String code, RoomView room) throws IOException {
        broadcast(code.toUpperCase(), "ROOM_STATE", room);
    }

    private void broadcast(String code, String type, Object payload) throws IOException {
        for (WebSocketSession session : roomSessions.getOrDefault(code, Collections.emptySet())) {
            if (session.isOpen()) {
                send(session, type, payload);
            }
        }
    }

    private void send(WebSocketSession session, String type, Object payload) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", type,
                "payload", payload
        ))));
    }

    private String extractCode(WebSocketSession session) {
        String path = session.getUri().getPath();
        return roomTemplate.match(path).get("code");
    }
}
