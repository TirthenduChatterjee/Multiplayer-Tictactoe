package com.example.tictactoe.service;

public class GameRuleException extends RuntimeException {

    public GameRuleException(String message) {
        super(message);
    }
}
