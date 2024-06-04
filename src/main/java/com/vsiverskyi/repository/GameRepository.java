package com.vsiverskyi.repository;

import com.vsiverskyi.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}
