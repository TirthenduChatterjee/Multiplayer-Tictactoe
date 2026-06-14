package com.example.tictactoe.service;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(String code) {
        super("Room " + code + " was not found.");
    }
}
