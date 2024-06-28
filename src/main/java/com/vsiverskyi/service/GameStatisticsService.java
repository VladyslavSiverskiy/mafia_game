package com.vsiverskyi.service;

import com.vsiverskyi.exception.ExceptionConstants;
import com.vsiverskyi.exception.NoGameWithSuchIdException;
import com.vsiverskyi.model.Game;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.repository.GameRepository;
import com.vsiverskyi.repository.GameStatisticsRepository;
import com.vsiverskyi.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class GameStatisticsService {

    private final GameRepository gameRepository;
    private final RoleRepository roleRepository;
    private final GameStatisticsRepository gameStatisticsRepository;

    public GameStatistics setInGameNickname(long gameId, int playerNumber, String inGameNickname) {
        // TODO: Mb need to add some other func cases
        GameStatistics gameStatistics = gameStatisticsRepository.findByGame_IdAndAndInGameNumber(gameId, playerNumber);
        gameStatistics.setInGameNickname(inGameNickname);
        return gameStatisticsRepository.save(gameStatistics);
    }

    public GameStatistics killPlayer(long gameId, int playerToKillInGameNumber) {
        GameStatistics gameStatistics = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(gameId, playerToKillInGameNumber);
        System.out.println(gameStatistics.getRole());
        if(gameStatistics.getRole().getRoleNameConstant().equals(ERoleOrder.STRILOCHNYK.name())) {
            System.out.println("HERE");
            gameStatistics.setTimesWasKilled((short) (gameStatistics.getTimesWasKilled() + 1));
        }else {
            gameStatistics.setInGame(false);
        }
        gameStatisticsRepository.save(gameStatistics);
        return gameStatistics;
    }

    /**
     * This method is used in penalty system, when player gets red card
     * */
    public GameStatistics removePlayerFromGame(long gameId, int playerToKillInGameNumber) {
        GameStatistics gameStatistics = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(gameId, playerToKillInGameNumber);
        gameStatistics.setInGame(false);
        gameStatisticsRepository.save(gameStatistics);
        return gameStatistics;
    }

    public GameStatistics healPlayer(long gameId, int playerToKillInGameNumber) {
        GameStatistics gameStatistics = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(gameId, playerToKillInGameNumber);
        gameStatistics.setInGame(true);
        gameStatistics.setTimesWasHealed((short) (gameStatistics.getTimesWasHealed() + 1));
        gameStatisticsRepository.save(gameStatistics);
        return gameStatistics;
    }


    public List<GameStatistics> getGameStatisticsByGameId(Long currentGameId) throws NoGameWithSuchIdException {
        Game game = gameRepository.findById(currentGameId).orElseThrow(() ->
                new NoGameWithSuchIdException(ExceptionConstants.NO_GAME_WITH_SUCH_ID + currentGameId));
        return game.getGameStatistics().stream()
                .sorted(Comparator.comparing(gs -> {
                    if (gs.getRole() == null) {
                        return ERoleOrder.UNDEFINED;
                    }
                    return ERoleOrder.fromName(gs.getRole().getRoleNameConstant());
                }))
                .collect(Collectors.toList());
    }

    public List<GameStatistics> getGameStatisticsByGameIdSortedByInGameNumber(Long currentGameId) throws NoGameWithSuchIdException {
        return getGameStatisticsByGameId(currentGameId).stream()
                .sorted(Comparator.comparing(GameStatistics::getInGameNumber))
                .collect(Collectors.toList());
    }

    public GameStatistics deletePlayerAfterVoting(Long gameId, int inGameNumber) {
        GameStatistics gameStatistics = gameStatisticsRepository.findByGame_IdAndAndInGameNumber(gameId, inGameNumber);
        gameStatistics.setInGame(false);
        gameStatistics = gameStatisticsRepository.save(gameStatistics);
        return gameStatistics;
    }

    public Boolean checkIfDonIsAlive(long currentGameId) {
        List<GameStatistics> gameStatisticsList =  gameRepository.findById(currentGameId).get().getGameStatistics();
        GameStatistics donRole = gameStatisticsList.stream()
                .filter(gameStatistics -> gameStatistics.getRole().getRoleNameConstant()
                        .equals(ERoleOrder.DON.name())).toList().get(0);
        return donRole.isInGame();
    }

    public void updateYellowCards(Long gameId, int inGameNumber, int yellowCards) {
        GameStatistics gameStatistics = gameStatisticsRepository.findByGame_IdAndAndInGameNumber(gameId, inGameNumber);
        if (gameStatistics != null) {
            gameStatistics.setYellowCards(yellowCards);
            gameStatisticsRepository.save(gameStatistics);
        }
    }

    public void removeAllVotingSkipsPerDay(Long currentGameId) {
        List<GameStatistics> gameStatisticsList = getGameStatisticsByGameId(currentGameId);
        for (GameStatistics gs: gameStatisticsList) {
            gs.setSkipNextVoting(false);
            gameStatisticsRepository.save(gs);
        }
    }

    public void blockVotingPerDay(Long currentGameId, int chosenPlayerNumber) {
        GameStatistics gameStatistics = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(currentGameId, chosenPlayerNumber);
        gameStatistics.setSkipNextVoting(true);
        gameStatisticsRepository.save(gameStatistics);
    }

    public int getSumOfStrilochnykAttempts(Long currentGameId) {
        int sum = getGameStatisticsByGameId(currentGameId)
                .stream()
                .filter(gameStatistics ->
                        gameStatistics.isInGame()
                        && gameStatistics.getRole().getRoleNameConstant().equals(ERoleOrder.STRILOCHNYK.name()))
                .mapToInt(GameStatistics::getTimesWasKilled)
                .sum();
        System.out.println(sum);
        return sum;
    }
}
