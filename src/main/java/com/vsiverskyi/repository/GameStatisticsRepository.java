package com.vsiverskyi.repository;

import com.vsiverskyi.model.GameStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameStatisticsRepository extends JpaRepository<GameStatistics, Long> {
    GameStatistics findByGame_IdAndAndInGameNumber(Long gameId, Integer playerNumber);
}
