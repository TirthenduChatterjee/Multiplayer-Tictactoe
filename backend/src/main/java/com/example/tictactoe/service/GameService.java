package com.example.tictactoe.service;

import com.example.tictactoe.dto.RoomSessionView;
import com.example.tictactoe.dto.RoomView;
import com.example.tictactoe.model.GameMove;
import com.example.tictactoe.model.GameRoom;
import com.example.tictactoe.model.GameStatus;
import com.example.tictactoe.repository.GameMoveRepository;
import com.example.tictactoe.repository.GameRoomRepository;
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int[][] WIN_LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };

    private final GameRoomRepository roomRepository;
    private final GameMoveRepository moveRepository;
    private final SecureRandom random = new SecureRandom();

    public GameService(GameRoomRepository roomRepository, GameMoveRepository moveRepository) {
        this.roomRepository = roomRepository;
        this.moveRepository = moveRepository;
    }

    @Transactional
    public RoomSessionView createRoom(String playerName) {
        GameRoom room = new GameRoom();
        room.setCode(generateCode());
        room.setXPlayerName(cleanName(playerName));
        GameRoom saved = roomRepository.save(room);
        return new RoomSessionView(RoomView.from(saved, "Room created. Share the code with a friend."), "X");
    }

    @Transactional
    public RoomSessionView joinRoom(String code, String playerName) {
        GameRoom room = findRoom(code);
        String cleanPlayerName = cleanName(playerName);

        if (cleanPlayerName.equals(room.getXPlayerName())) {
            return new RoomSessionView(RoomView.from(room, "Welcome back."), "X");
        }

        if (room.getOPlayerName() == null) {
            room.setOPlayerName(cleanPlayerName);
            room.setStatus(GameStatus.IN_PROGRESS);
            room.touch();
            return new RoomSessionView(RoomView.from(roomRepository.save(room), "Joined room."), "O");
        }

        if (cleanPlayerName.equals(room.getOPlayerName())) {
            return new RoomSessionView(RoomView.from(room, "Welcome back."), "O");
        }

        throw new GameRuleException("This room already has two players.");
    }

    @Transactional(readOnly = true)
    public RoomView getRoom(String code) {
        return RoomView.from(findRoom(code), null);
    }

    @Transactional
    public RoomView makeMove(String code, int cellIndex, String playerName) {
        GameRoom room = findRoom(code);

        if (room.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameRuleException("The game is not ready for moves.");
        }

        String symbol = resolveSymbol(room, cleanName(playerName));
        if (!room.getNextTurn().equals(symbol)) {
            throw new GameRuleException("It is " + room.getNextTurn() + "'s turn.");
        }

        char[] board = room.getBoard().toCharArray();
        if (cellIndex < 0 || cellIndex > 8 || board[cellIndex] != '-') {
            throw new GameRuleException("That cell is not available.");
        }

        board[cellIndex] = symbol.charAt(0);
        room.setBoard(new String(board));
        room.setStatus(statusFor(board));
        if (room.getStatus() == GameStatus.IN_PROGRESS) {
            room.setNextTurn(symbol.equals("X") ? "O" : "X");
        }
        room.touch();

        GameMove move = new GameMove();
        move.setRoom(room);
        move.setCellIndex(cellIndex);
        move.setSymbol(symbol);
        move.setPlayerName(cleanName(playerName));
        moveRepository.save(move);

        return RoomView.from(roomRepository.save(room), null);
    }

    @Transactional
    public RoomView restart(String code, String playerName) {
        GameRoom room = findRoom(code);
        resolveSymbol(room, cleanName(playerName));
        room.setBoard("---------");
        room.setNextTurn("X");
        room.setStatus(room.getOPlayerName() == null ? GameStatus.WAITING : GameStatus.IN_PROGRESS);
        room.touch();
        return RoomView.from(roomRepository.save(room), "Game restarted.");
    }

    private GameRoom findRoom(String code) {
        return roomRepository.findByCode(code.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new RoomNotFoundException(code));
    }

    private String resolveSymbol(GameRoom room, String playerName) {
        if (playerName.equals(room.getXPlayerName())) {
            return "X";
        }
        if (playerName.equals(room.getOPlayerName())) {
            return "O";
        }
        throw new GameRuleException("You are not a player in this room.");
    }

    private GameStatus statusFor(char[] board) {
        for (int[] line : WIN_LINES) {
            char value = board[line[0]];
            if (value != '-' && value == board[line[1]] && value == board[line[2]]) {
                return value == 'X' ? GameStatus.X_WON : GameStatus.O_WON;
            }
        }
        for (char value : board) {
            if (value == '-') {
                return GameStatus.IN_PROGRESS;
            }
        }
        return GameStatus.DRAW;
    }

    private String generateCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                builder.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = builder.toString();
        } while (roomRepository.existsByCode(code));
        return code;
    }

    private String cleanName(String playerName) {
        return playerName == null ? "" : playerName.trim();
    }
}
