package com.vsiverskyi.repository;

import com.vsiverskyi.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {

    // Find games with lastUpdate today
    // Find games with lastUpdate within a specific date and time range
    @Query("SELECT g FROM Game g WHERE g.lastUpdate BETWEEN :startOfDay AND :endOfDay")
    List<Game> findGamesByDateRange(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    // Alternatively, if you need to limit the number of results
    @Query(value = "SELECT * FROM games g WHERE g.last_update >= :startOfDay ORDER BY g.last_update DESC LIMIT :limit", nativeQuery = true)
    List<Game> findRecentGames(@Param("startOfDay") LocalDateTime startOfDay, @Param("limit") int limit);

    @Query(value = "SELECT * FROM games g WHERE g.game_status = 'WAS_COMPLETED' ORDER BY g.last_update DESC LIMIT :limit", nativeQuery = true)
    List<Game> findRecentCompletedGames(@Param("limit") int limit);
}
