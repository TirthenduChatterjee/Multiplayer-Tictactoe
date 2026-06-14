package com.example.tictactoe.dto;

import com.example.tictactoe.model.GameRoom;
import com.example.tictactoe.model.GameStatus;
import java.time.Instant;
import java.util.List;

public record RoomView(
        String code,
        List<String> board,
        String nextTurn,
        GameStatus status,
        PlayerView xPlayer,
        PlayerView oPlayer,
        Instant createdAt,
        Instant updatedAt,
        String message
) {

    public static RoomView from(GameRoom room, String message) {
        return new RoomView(
                room.getCode(),
                room.getBoard().chars().mapToObj(value -> String.valueOf((char) value)).toList(),
                room.getNextTurn(),
                room.getStatus(),
                new PlayerView(room.getXPlayerName(), "X"),
                room.getOPlayerName() == null ? null : new PlayerView(room.getOPlayerName(), "O"),
                room.getCreatedAt(),
                room.getUpdatedAt(),
                message
        );
    }
}
