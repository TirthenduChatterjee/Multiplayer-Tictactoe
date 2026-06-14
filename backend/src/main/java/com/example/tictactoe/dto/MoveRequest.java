package com.example.tictactoe.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MoveRequest(
        @Min(0) @Max(8) int cellIndex,
        @NotBlank String playerName
) {
}
