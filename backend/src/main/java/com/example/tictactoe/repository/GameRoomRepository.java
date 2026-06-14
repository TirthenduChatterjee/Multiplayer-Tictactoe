package com.example.tictactoe.repository;

import com.example.tictactoe.model.GameRoom;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {

    Optional<GameRoom> findByCode(String code);

    boolean existsByCode(String code);
}
