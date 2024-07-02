package com.vsiverskyi.service;

import com.vsiverskyi.exception.ExceptionConstants;
import com.vsiverskyi.exception.NoGameWithSuchIdException;
import com.vsiverskyi.model.Game;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.model.enums.ETeam;
import com.vsiverskyi.repository.GameRepository;
import com.vsiverskyi.repository.GameStatisticsRepository;
import com.vsiverskyi.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PointsService {

    private final GameRepository gameRepository;
    private final GameStatisticsService gameStatisticsService;
    private final RoleRepository roleRepository;
    private final GameStatisticsRepository gameStatisticsRepository;

    public void countPointsAfterGameWasFinished(Long gameId) {
        //Нарахувати переможній стороні
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NoGameWithSuchIdException(ExceptionConstants.NO_GAME_WITH_SUCH_ID + gameId));
        List<GameStatistics> gamers = gameStatisticsService.getGameStatisticsByGameId(gameId);
        ETeam winnerTeam = game.getWinnerSide();
        List<GameStatistics> winners = gamers
                .stream()
                .filter(gameStatistics -> gameStatistics.getRole().getTeam().equals(winnerTeam))
                .collect(Collectors.toList());
        countPointsForWinnersTeamAfterGameWasFinished(winners);
    }

    public void countOnePointAfterDayAndNight(Long gameId) {
        List<GameStatistics> gamers = gameStatisticsService.getGameStatisticsByGameId(gameId);
        for (GameStatistics gameStatistics: gamers) {
            if (gameStatistics.isInGame()) {
                gameStatistics.setPoints(gameStatistics.getPoints() + 1);
            }
        }
    }

    /**
     * @param gameId - id поточної гри
     * @param actionPlayerRole - гравці з такою роллю здійснили хід, їм будуть нараховані очки
     * @param targetPlayerInGameNumber - ігровий номер гравця, в якого голосують вночі
     */
    public void countPointsInOrderToNightAction(Long gameId, Role actionPlayerRole, Integer targetPlayerInGameNumber) {
        List<GameStatistics> gameStatisticsList = gameStatisticsService.getGameStatisticsByGameId(gameId);
        // Ситуація, коли мирний вкидає в мафію
        if (actionPlayerRole.getTeam().equals(ETeam.PEACE)
            && gameStatisticsRepository
                    .findByGame_IdAndAndInGameNumber(gameId, targetPlayerInGameNumber)
                    .getRole().getTeam().equals(ETeam.MAFIA)) {
            for (GameStatistics gameStatistics: gameStatisticsList) {
                if (gameStatistics.getRole().equals(actionPlayerRole)) {
                    gameStatistics.setPoints(gameStatistics.getPoints() + 4);
                    gameStatisticsRepository.save(gameStatistics);
                }
            }
        }

        // Нарахування балів мафії
        if (actionPlayerRole.getTeam().equals(ETeam.MAFIA)
            && gameStatisticsRepository
                    .findByGame_IdAndAndInGameNumber(gameId, targetPlayerInGameNumber)
                    .getRole().getTeam().equals(ETeam.PEACE)) {
            for (GameStatistics gameStatistics: gameStatisticsList) {
                if (gameStatistics.getRole().getTeam().equals(ETeam.MAFIA)) {
                    gameStatistics.setPoints(gameStatistics.getPoints() + 2);
                    gameStatisticsRepository.save(gameStatistics);
                }
            }
        }

    }

    private void countPointsForWinnersTeamAfterGameWasFinished(List<GameStatistics> winnersList) {
        List<GameStatistics> aliveWinnersList = winnersList.stream().filter(GameStatistics::isInGame).toList();
        if (aliveWinnersList.size() == winnersList.size()) {
            for (GameStatistics winner: winnersList) {
                winner.setPoints(winner.getPoints() + 10);
                gameStatisticsRepository.save(winner);
            }
        } else {
            for (GameStatistics winner: winnersList) {
                if(winner.isInGame() && winner.getRole().equals(ERoleOrder.PEACE)) {
                    winner.setPoints(winner.getPoints() + 10);
                }else if (winner.isInGame()){
                    winner.setPoints(winner.getPoints() + 5);
                } else {
                    winner.setPoints(winner.getPoints() + 3);
                }
                gameStatisticsRepository.save(winner);
            }
        }
    }

    public void countPointsInOrderToDayAction(Long gameId, int targetPlayerNumber, int voterPlayerNumber) {
        GameStatistics voterPlayer = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(gameId, voterPlayerNumber);
        GameStatistics targetPlayer = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(gameId, targetPlayerNumber);

        //Нарахування балів
        if (voterPlayer.getRole().getTeam().equals(ETeam.PEACE)
            && targetPlayer.getRole().getTeam().equals(ETeam.MAFIA)) {
            voterPlayer.setPoints(voterPlayer.getPoints() + 3); // TODO: можливо змінити кількість балів
            gameStatisticsRepository.save(voterPlayer);
        } else if(voterPlayer.getRole().getTeam().equals(ETeam.MAFIA)
                  && targetPlayer.getRole().getTeam().equals(ETeam.PEACE)) {
            voterPlayer.setPoints(voterPlayer.getPoints() + 2); // TODO: можливо змінити кількість балів
            gameStatisticsRepository.save(voterPlayer);
        }

    }
}
