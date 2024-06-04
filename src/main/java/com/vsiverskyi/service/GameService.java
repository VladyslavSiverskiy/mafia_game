package com.vsiverskyi.service;

import com.vsiverskyi.model.Game;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.model.enums.EGameStatus;
import com.vsiverskyi.repository.GameRepository;
import com.vsiverskyi.repository.GameStatisticsRepository;
import com.vsiverskyi.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final RoleRepository roleRepository;
    private final GameStatisticsRepository gameStatisticsRepository;

    public Game getGameInfo(Long id) {
        return gameRepository.findById(id).get();
    }

    public Game beginGame(Integer playersAmount, Map<String, Integer> rolesAmount) { // Mafia - 3, Doctor - 2

        // TODO: взяти ролі з репозиторію відповідно до ключа мапи
        Role mafiaRole = Role.builder()
                .title("Мафія")
                .build();
        Role donRole = Role.builder()
                .title("Дон")
                .build();
        Role doctorRole = Role.builder()
                .title("Лікар")
                .build();

        roleRepository.save(mafiaRole);
        roleRepository.save(donRole);
        roleRepository.save(doctorRole);
//        TODO: ці збереження повинні бути ще до початку гри

        Game game = Game.builder()
                .gameStatus(EGameStatus.IN_PROCESS)
                .playersAmount(playersAmount)
                .lastUpdate(LocalDateTime.now())
                .build();
        game = gameRepository.save(game);

        // TODO: циклом йдемо по мапі і роздаємо відповідну кількість даних
        //  player тут поки нема
        GameStatistics gameStatisticsFirstPlayer = GameStatistics.builder()
                .game(game)
                .role(mafiaRole)
                .build();
        gameStatisticsRepository.save(gameStatisticsFirstPlayer);
        GameStatistics gameStatisticsSecondPlayer = GameStatistics.builder()
                .game(game)
                .role(doctorRole)
                .build();
        gameStatisticsRepository.save(gameStatisticsSecondPlayer);
        GameStatistics gameStatisticsThirdPlayer = GameStatistics.builder()
                .game(game)
                .role(donRole)
                .build();
        gameStatisticsRepository.save(gameStatisticsThirdPlayer);

        // TODO: зробити цикл який всіх інших мирних проініціалізує
        return game;
    }
}
