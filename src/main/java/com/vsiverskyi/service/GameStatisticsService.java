package com.vsiverskyi.service;

import com.vsiverskyi.exception.ExceptionConstants;
import com.vsiverskyi.exception.NoGameWithSuchIdException;
import com.vsiverskyi.model.Game;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.repository.GameRepository;
import com.vsiverskyi.repository.GameStatisticsRepository;
import com.vsiverskyi.repository.RoleRepository;
import com.vsiverskyi.utils.Action;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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

    public GameStatistics findByInGameNumberAndGameId(int inGameNumber, long gameId) {
        return gameStatisticsRepository.findByGame_IdAndAndInGameNumber(gameId, inGameNumber);
    }

    public List<GameStatistics> getMarkedByBombAlivePlayersInGameNumbers(Long gameId) {
        return gameRepository.findById(gameId).get()
                .getGameStatistics()
                .stream()
                .filter(gameStatistics -> gameStatistics.isWasMarkedByBomb() && gameStatistics.isInGame())
                .toList();
    }

    public Action markPlayersByBomb(Set<Integer> playersToMark, Long gameId) {
        clearPreviousMarks(gameId);

        List<String> markedNicknames = new ArrayList<>();
        for (Integer playerInGameNumber : playersToMark) {
            GameStatistics gameStatistics = gameStatisticsRepository
                    .findByGame_IdAndAndInGameNumber(gameId, playerInGameNumber);
            gameStatistics.setWasMarkedByBomb(true);
            gameStatisticsRepository.save(gameStatistics);
            markedNicknames.add(gameStatistics.getInGameNickname());
        }
        Action action = new Action();
        action.setActionText(ERoleOrder.BOMBA.getTitle() + " мінує гравців " + markedNicknames);
        return action;
    }

    private void clearPreviousMarks(Long gameId) {
        List<GameStatistics> gameStatistics = gameRepository.findById(gameId).get().getGameStatistics();
        for (GameStatistics gameStatistic : gameStatistics) {
            gameStatistic.setWasMarkedByBomb(false);
            gameStatisticsRepository.save(gameStatistic);
        }
    }

    private void clearHealedOnThePreviousStage(Long gameId) {
        List<GameStatistics> gameStatistics = gameRepository.findById(gameId).get().getGameStatistics();
        for (GameStatistics gameStatistic : gameStatistics) {
            gameStatistic.setHeadledOnThePreviousStage(false);
            gameStatisticsRepository.save(gameStatistic);
        }
    }

    public void killMarkedByBombPlayers(long gameId) {
        List<GameStatistics> gamersMarkedByBomb = gameRepository.findById(gameId).get().getGameStatistics();
        for (GameStatistics gamer : gamersMarkedByBomb) {
            if (gamer.isWasMarkedByBomb()) {
                gamer.setInGame(false);
                gameStatisticsRepository.save(gamer);
            }
        }
    }

    public GameStatistics killPlayer(long gameId, int playerToKillInGameNumber) {
        GameStatistics gameStatistics = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(gameId, playerToKillInGameNumber);
        if (gameStatistics.getRole().getRoleNameConstant().equals(ERoleOrder.STRILOCHNYK.name())
            && !(gameStatistics.isPoisonedByLady() && gameStatistics.getNightsTillDeath() == 0)) {
            gameStatistics.setTimesWasKilled((short) (gameStatistics.getTimesWasKilled() + 1));
        } else {
            gameStatistics.setInGame(false);
            gameStatistics.setTimesWasKilled((short) (gameStatistics.getTimesWasKilled() + 1));
        }
        gameStatisticsRepository.save(gameStatistics);
        return gameStatistics;
    }

    /**
     * This method is used in penalty system, when player gets red card
     */
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
        clearHealedOnThePreviousStage(gameId);
        gameStatistics.setPoisonedByLady(false);
        gameStatistics.setHeadledOnThePreviousStage(true);
        gameStatistics.setTimesWasHealed((short) (gameStatistics.getTimesWasHealed() + 1));
        if (gameStatistics.getTimesWasKilled() > 1
            || (
                    gameStatistics.getTimesWasKilled() == 1
                    && gameStatistics.isPoisonedByLady() && gameStatistics.getNightsTillDeath() == 0)
        ) {
            gameStatistics.setInGame(false);
        } else {
            gameStatistics.setInGame(true);
        }
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
        if (!gameStatistics.isDefendedPerNextVoting()) {
            gameStatistics.setInGame(false);
            gameStatistics = gameStatisticsRepository.save(gameStatistics);
        }

        if(gameStatistics.getRole().getTitle().equals(ERoleOrder.ZATYCHKA_SUDDYA.getTitle()) && !gameStatistics.isDefendedPerNextVoting()) {
            killMarkedBySuddyaPlayer(gameId);
        }

        if (gameStatistics.getRole().getTitle().equals(ERoleOrder.BOMBA.getTitle())) {
//            killNearestPlayers(gameStatistics.getInGameNumber(), gameId);
            killMarkedByBombPlayers(gameId);
        }
        return gameStatistics;
    }

    private void killMarkedBySuddyaPlayer(Long gameId) {
        List<GameStatistics> gameStatisticsList = gameRepository.findById(gameId).get().getGameStatistics();
        for (GameStatistics gameStatistics: gameStatisticsList) {
            if (gameStatistics.isSkipNextVotingBecauseOfSuddya()) {
                gameStatistics.setInGame(false);
                gameStatisticsRepository.save(gameStatistics);
            }
        }
    }

    private void killNearestPlayers(Integer bombInGameNumber, Long gameId) {
        int killedPlayersFromLeftSide = 0;
        int killedPlayersFromRightSide = 0;

        int amountOfPlayers = gameRepository.findById(gameId).get().getPlayersAmount();
        int currentPlayerNumberToCheck = bombInGameNumber + 1;

        //do right
        while (killedPlayersFromRightSide < 2) {
            if (currentPlayerNumberToCheck > amountOfPlayers) {
                currentPlayerNumberToCheck = 1;
            }
            GameStatistics gameStatistics = gameStatisticsRepository
                    .findByGame_IdAndAndInGameNumber(gameId, currentPlayerNumberToCheck);
            if (gameStatistics.isInGame()) {
                gameStatistics.setInGame(false);
                gameStatisticsRepository.save(gameStatistics);
                killedPlayersFromRightSide++;
                currentPlayerNumberToCheck++;
            } else {
                currentPlayerNumberToCheck++;
            }
        }

        //do left
        currentPlayerNumberToCheck = bombInGameNumber - 1;
        while (killedPlayersFromLeftSide < 2) {
            if (currentPlayerNumberToCheck > amountOfPlayers) {
                currentPlayerNumberToCheck = amountOfPlayers;
            }
            GameStatistics gameStatistics = gameStatisticsRepository
                    .findByGame_IdAndAndInGameNumber(gameId, currentPlayerNumberToCheck);
            if (gameStatistics.isInGame()) {
                gameStatistics.setInGame(false);
                gameStatisticsRepository.save(gameStatistics);
                killedPlayersFromLeftSide++;
                currentPlayerNumberToCheck--;
            } else {
                currentPlayerNumberToCheck--;
            }
        }
    }

    public Boolean checkIfDonIsAlive(long currentGameId) {
        List<GameStatistics> gameStatisticsList = gameRepository.findById(currentGameId).get().getGameStatistics();
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
        for (GameStatistics gs : gameStatisticsList) {
            gs.setSkipNextVoting(false);
            gs.setSkipNextVotingBecauseOfSuddya(false);
            gameStatisticsRepository.save(gs);
        }
    }

    public void removeAllVotingDefencesPerDay(Long currentGameId) {
        List<GameStatistics> gameStatisticsList = getGameStatisticsByGameId(currentGameId);
        for (GameStatistics gs : gameStatisticsList) {
            gs.setDefendedPerNextVoting(false);
            gameStatisticsRepository.save(gs);
        }
    }

    public void blockVotingPerDay(Long currentGameId, int chosenPlayerNumber) {
        GameStatistics gameStatistics = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(currentGameId, chosenPlayerNumber);
        gameStatistics.setSkipNextVoting(true);
        gameStatisticsRepository.save(gameStatistics);
    }
    public void suddyaBlockVotingPerDay(Long currentGameId, int chosenPlayerNumber) {
        GameStatistics gameStatistics = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(currentGameId, chosenPlayerNumber);
        gameStatistics.setSkipNextVotingBecauseOfSuddya(true);
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
        return sum;
    }

    public void removeYellowCard(Long currentGameId, int playerInGameNumber) {
        GameStatistics gameStatistics = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(currentGameId, playerInGameNumber);
        gameStatistics.setYellowCards(gameStatistics.getYellowCards() - 1);
        gameStatisticsRepository.save(gameStatistics);
    }

    public void removeRedCard(Long currentGameId, int playerInGameNumber) {
        GameStatistics gameStatistics = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(currentGameId, playerInGameNumber);
        gameStatistics.setRedCards((byte) (gameStatistics.getRedCards() - 1));
        gameStatistics.setInGame(true);
        gameStatisticsRepository.save(gameStatistics);
    }

    public void resetYellowCardsAmountAndGiveRedOne(Long currentGameId, int playerNumber) {
        GameStatistics gameStatistics = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(currentGameId, playerNumber);
        gameStatistics.setYellowCards(0);
        gameStatistics.setRedCards((byte) 1);
        gameStatisticsRepository.save(gameStatistics);
    }

    public void setDefenceForNextVoting(Long currentGameId, int chosenPlayerNumber) {
        GameStatistics gameStatistics = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(currentGameId, chosenPlayerNumber);
        gameStatistics.setDefendedPerNextVoting(true);
        gameStatisticsRepository.save(gameStatistics);
    }

    public void resetDefenceForNextVoting(Long currentGameId, int chosenPlayerNumber) {
        GameStatistics gameStatistics = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(currentGameId, chosenPlayerNumber);
        gameStatistics.setDefendedPerNextVoting(false);
        gameStatisticsRepository.save(gameStatistics);
    }

    public void save(GameStatistics gameStatistics) {
        gameStatisticsRepository.save(gameStatistics);
    }

    public List<GameStatistics> processPoisonedPlayers(Long currentGameId) {
        List<GameStatistics> poisonedGamers = gameRepository.findById(currentGameId)
                .orElseThrow()
                .getGameStatistics().stream().filter(gameStatistics -> gameStatistics.isPoisonedByLady()).collect(Collectors.toList());
        List<GameStatistics> killedDueToPoisoningInGameNumbers = new ArrayList<>();
        for (GameStatistics gameStatistics : poisonedGamers) {
            System.out.println(gameStatistics);
            if (gameStatistics.getNightsTillDeath() == 0) {
                killPlayer(currentGameId, gameStatistics.getInGameNumber());
//                gameStatistics.setTimesWasKilled((short) (gameStatistics.getTimesWasKilled() + 1));
                killedDueToPoisoningInGameNumbers.add(gameStatistics);
            } else {
                gameStatistics.setNightsTillDeath((short) (gameStatistics.getNightsTillDeath() - 1));
                gameStatisticsRepository.save(gameStatistics);
            }
        }
        return killedDueToPoisoningInGameNumbers;
    }

    public Action markPlayerByKradiy(int chosenPlayerNumber, Long currentGameId) {
        GameStatistics gameStatistics = gameStatisticsRepository
                .findByGame_IdAndAndInGameNumber(currentGameId, chosenPlayerNumber);
        gameStatistics.setWasMarkedByKradiy(true);
        gameStatisticsRepository.save(gameStatistics);
        Action action = new Action();
        action.setActionText(ERoleOrder.KRADIY.getTitle() + " забирає голос в гравця " + gameStatistics.getInGameNickname());
        action.setLocalDateTime(LocalDateTime.now());
        return action;
    }

    public void removeAllKradiyChoices(Long currentGameId) {
        List<GameStatistics> gameStatisticsList = gameRepository.findById(currentGameId)
                .orElseThrow(() -> new NoGameWithSuchIdException(ExceptionConstants.NO_GAME_WITH_SUCH_ID)).getGameStatistics();
        for (GameStatistics gameStatistics : gameStatisticsList) {
            gameStatistics.setWasMarkedByKradiy(false);
            gameStatisticsRepository.save(gameStatistics);
        }
    }

    public boolean checkIfAliveOrDontHaveRedCardsWithRole(Role currentRole, Long currentGameId) {
        return !gameRepository.findById(currentGameId).get().getGameStatistics()
                .stream()
                .filter(gameStatistics ->
                        gameStatistics.getRole().getRoleNameConstant()
                                .equals(currentRole.getRoleNameConstant()) && gameStatistics.getRedCards() <= 0)
                .toList().isEmpty();
    }

    public boolean checkIfPlayerWasHealed(int chosenPlayerNumber, Long currentGameId) {
        return gameStatisticsRepository.findByGame_IdAndAndInGameNumber(currentGameId, chosenPlayerNumber).isHeadledOnThePreviousStage();
    }

    public boolean checkIfBombIsAlive(Long currentGameId) {
        List<GameStatistics> gameStatisticsList = gameRepository
                .findById(currentGameId).get()
                .getGameStatistics();

        Optional<GameStatistics> bomb = gameStatisticsList
                .stream()
                .filter(gameStatistics -> gameStatistics.getRole().getRoleNameConstant().equals(ERoleOrder.BOMBA.name())).findFirst();

        if (bomb.isPresent()) {
            return bomb.get().isInGame();
        } else {
            return false;
        }
    }

    public boolean checkIfBombIsAddedToGame(Long currentGameId) {
        List<GameStatistics> gameStatisticsList = gameRepository
                .findById(currentGameId).get()
                .getGameStatistics();

        return !gameStatisticsList
                .stream()
                .filter(gameStatistics -> gameStatistics.getRole().getRoleNameConstant()
                        .equals(ERoleOrder.BOMBA.name())).toList().isEmpty();
    }

    public void updateGameStatistics(List<GameStatistics> gameStatistics) {
        for (GameStatistics gameStatistic : gameStatistics) {
            gameStatisticsRepository.save(gameStatistic);
        }
    }

    public GameStatistics findMarkedByKradiy(Long currentGameId) {
        for (GameStatistics gameStatistic : gameRepository.findById(currentGameId).get().getGameStatistics()) {
            if (gameStatistic.isWasMarkedByKradiy()) {
                return gameStatistic;
            }
        }
        return null;
    }

    public GameStatistics findMarkedBySuddyaByGameId(Long currentGameId) {
        for (GameStatistics gameStatistic : gameRepository.findById(currentGameId).get().getGameStatistics()) {
            if (gameStatistic.isSkipNextVotingBecauseOfSuddya()) {
                return gameStatistic;
            }
        }
        return null;
    }

    public void setSkipNextVoting(GameStatistics gs) {
        gs.setSkipNextVoting(true);
        gameStatisticsRepository.save(gs);
    }

    public void undoSkipNextVoting(GameStatistics gs) {
        gs.setSkipNextVoting(true);
        gameStatisticsRepository.save(gs);
    }

    public boolean checkIfCurrentRoleHaveAvailableOfRedCardPlayers(Long currentGameId, Role currentRole) {
        List<GameStatistics> gameStatisticsListWithSelectedRole = getGameStatisticsByGameId(currentGameId)
                .stream()
                .filter(gameStatistics -> gameStatistics.getRole()
                        .getRoleNameConstant().equals(currentRole.getRoleNameConstant())).toList();

        for (GameStatistics gameStatistics: gameStatisticsListWithSelectedRole) {
            if (gameStatistics.getRedCards() == 0) {
                return true;
            }
        }
        return false;
    }
}
