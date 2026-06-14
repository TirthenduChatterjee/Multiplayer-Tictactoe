package com.example.tictactoe.repository;

import com.example.tictactoe.model.GameMove;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameMoveRepository extends JpaRepository<GameMove, Long> {
}
