package com.example.tictactoe.controller;

import com.example.tictactoe.dto.CreateRoomRequest;
import com.example.tictactoe.dto.JoinRoomRequest;
import com.example.tictactoe.dto.RoomSessionView;
import com.example.tictactoe.dto.RoomView;
import com.example.tictactoe.service.GameRuleException;
import com.example.tictactoe.service.GameService;
import com.example.tictactoe.service.RoomNotFoundException;
import com.example.tictactoe.websocket.GameWebSocketHandler;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class RoomController {

    private final GameService gameService;
    private final GameWebSocketHandler gameWebSocketHandler;

    public RoomController(GameService gameService, GameWebSocketHandler gameWebSocketHandler) {
        this.gameService = gameService;
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    @PostMapping
    public RoomSessionView createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return gameService.createRoom(request.playerName());
    }

    @PostMapping("/{code}/join")
    public RoomSessionView joinRoom(@PathVariable String code, @Valid @RequestBody JoinRoomRequest request) {
        RoomSessionView session = gameService.joinRoom(code, request.playerName());
        try {
            gameWebSocketHandler.broadcastRoomState(session.room().code(), session.room());
        } catch (Exception ignored) {
            // The joining player still receives the room state in the HTTP response.
        }
        return session;
    }

    @GetMapping("/{code}")
    public RoomView getRoom(@PathVariable String code) {
        return gameService.getRoom(code);
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<Map<String, String>> roomNotFound(RoomNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(GameRuleException.class)
    public ResponseEntity<Map<String, String>> gameRule(GameRuleException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
