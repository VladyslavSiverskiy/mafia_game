package com.vsiverskyi.service;

import com.vsiverskyi.exception.CantStartGameException;
import com.vsiverskyi.exception.ExceptionConstants;
import com.vsiverskyi.exception.NoGameWithSuchIdException;
import com.vsiverskyi.exception.NoRoleWithSuchTitleException;
import com.vsiverskyi.model.Game;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.model.enums.EGameStatus;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.model.enums.ETeam;
import com.vsiverskyi.repository.GameRepository;
import com.vsiverskyi.repository.GameStatisticsRepository;
import com.vsiverskyi.repository.RoleRepository;
import com.vsiverskyi.utils.Action;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final GameStatisticsService gameStatisticsService;
    private final RoleRepository roleRepository;
    private final GameStatisticsRepository gameStatisticsRepository;
    private int playerNumber = 1;

    public Game getGameInfo(Long id) {
        return gameRepository.findById(id).get();
    }

    public Game beginGame(
            Integer playersAmount
    ) throws CantStartGameException {
        Game game = Game.builder()
                .gameStatus(EGameStatus.IN_PROCESS)
                .playersAmount(playersAmount)
                .lastUpdate(LocalDateTime.now())
                .build();
        game = gameRepository.save(game);

        // Напевно цей метод повинен повернути список з ID-ролей на гру,
        // по якому потім буде ітерація в SelectionRole
        return game;
    }

    public ETeam finishGameDueToTechnicalLoose(Long currentGameId, String teamName) {
        ETeam looserTeam = ETeam.valueOf(teamName);
        Game gameToFinish = getGameInfo(currentGameId);
        if (looserTeam == ETeam.PEACE) {
            gameToFinish.setWinnerSide(ETeam.MAFIA);
        } else {
            gameToFinish.setWinnerSide(ETeam.PEACE);
        }
        gameToFinish.setGameStatus(EGameStatus.WAS_COMPLETED);
        gameToFinish.setLastUpdate(LocalDateTime.now());
        gameToFinish = gameRepository.save(gameToFinish);
        return gameToFinish.getWinnerSide();
    }

    /**
     * @return List of roles id
     */
    public List<Integer> initRolesPerGame(
            Integer playersAmount, Integer mafiaAmount, Map<String, Integer> additionalRoles, Game game
    ) throws CantStartGameException {

//        List<String> peaceRolesToAdd = additionalRoles.entrySet().stream()
//                .filter(Map.Entry::getValue) // Filter entries with true values
//                .map(Map.Entry::getKey) // Map to keys
//                .collect(Collectors.toList()); // Collect to list


//        if (playersAmount < mafiaAmount + peaceRolesToAdd.size()) {
//            throw new CantStartGameException(ExceptionConstants.TOTAL_AMOUNT_OF_PLAYERS_IS_LOWER);
//        }
        if (mafiaAmount >= playersAmount - mafiaAmount) {
            throw new CantStartGameException(ExceptionConstants.MAFIA_AMOUNT_IS_HIGHER_THAN_PEACE);
        }
        List<Integer> rolesIdToReturnList = new ArrayList<>();
        rolesIdToReturnList.addAll(returnMafiaSidePlayersId(mafiaAmount, game));
        //TODO: refactor this method to accept amount of doctors and so on
        rolesIdToReturnList.addAll(returnPeaceSidePlayersId(playersAmount - mafiaAmount, additionalRoles, game));
        return rolesIdToReturnList;
    }

    private List<Integer> returnPeaceSidePlayersId(int peaceAmount, Map<String, Integer> additionalPeaceRoles, Game game) {
        List<Integer> peaceRolesIdList = new ArrayList<>();
        int amountOfPeacePlayers = peaceAmount;
        //create additional peace roles

        for (Map.Entry<String, Integer> additionalRolesMap : additionalPeaceRoles.entrySet()) {
            for (int i = 0; i < additionalRolesMap.getValue(); i++) {
                Role peaceRole =
                        roleRepository
                                .findByTitle(additionalRolesMap.getKey())
                                .orElseThrow(() ->
                                        new NoRoleWithSuchTitleException(ExceptionConstants.NO_ROLE_WITH_SUCH_TITLE + additionalRolesMap.getKey()));
                GameStatistics peacePlayer = GameStatistics.builder()
                        .game(game)
                        .inGameNumber(playerNumber++)
                        .inGame(true)
                        .points(0)
                        .yellowCards(0)
                        .redCards((byte) 0)
                        .build();
                gameStatisticsRepository.save(peacePlayer);
                peaceRolesIdList.add(peaceRole.getId());
                amountOfPeacePlayers--;

                // init team
                if (peaceRole.getTeam() == null) {
                    peaceRole.setTeam(ETeam.PEACE);
                    roleRepository.save(peaceRole);
                }
            }
        }

        //create peace
        for (int i = 0; i < amountOfPeacePlayers; i++) {
            Role peaceRole =
                    roleRepository
                            .findByTitle("Мирний")
                            .orElseThrow(() ->
                                    new NoRoleWithSuchTitleException(ExceptionConstants.NO_ROLE_WITH_SUCH_TITLE + "Мирний"));
            GameStatistics peacePlayer = GameStatistics.builder()
                    .game(game)
                    .inGame(true)
                    .inGameNumber(playerNumber++)
                    .points(0)
                    .yellowCards(0)
                    .redCards((byte) 0)
                    .build();
            gameStatisticsRepository.save(peacePlayer);
            peaceRolesIdList.add(peaceRole.getId());
        }
        return peaceRolesIdList;
    }

    private List<Integer> returnMafiaSidePlayersId(int mafiaAmount, Game game) {
        Role donRole = roleRepository.findByRoleNameConstant(ERoleOrder.DON.name());
        Role mafiaRole = roleRepository.findByRoleNameConstant(ERoleOrder.MAFIA.name());

        List<Integer> mafiaRoleIdList = new ArrayList<>();
        GameStatistics donPlayer = GameStatistics.builder()
                .game(game)
                .inGameNumber(playerNumber++)
                .inGame(true)
                .points(0)
                .build();
        gameStatisticsRepository.save(donPlayer);
        mafiaRoleIdList.add(donRole.getId());
        for (int i = 0; i < mafiaAmount - 1; i++) { // - 1 бо дона забираємо
            GameStatistics mafiaPlayer = GameStatistics.builder()
                    .game(game)
                    .inGameNumber(playerNumber++)
                    .inGame(true)
                    .points(0)
                    .build();
            gameStatisticsRepository.save(mafiaPlayer);
            mafiaRoleIdList.add(mafiaRole.getId());
        }

        // init teams for mafia players
        if (donRole.getTeam() == null) {
            donRole.setTeam(ETeam.MAFIA);
        }
        if (mafiaRole.getTeam() == null) {
            mafiaRole.setTeam(ETeam.MAFIA);
        }

        return mafiaRoleIdList;
    }

    public boolean checkIfGameIsOver(Long gameId) {
        // Гра закінчується якщо виграла мафія (або рівно мафії і мирних, або мафів більше)
        // Або всіх мафій вбили
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NoGameWithSuchIdException(ExceptionConstants.NO_GAME_WITH_SUCH_ID + gameId));
        if (checkIfMafiaAmountIsEqualsToPeaceAmount(gameId)) {
            game.setLastUpdate(LocalDateTime.now());
            game.setGameStatus(EGameStatus.WAS_COMPLETED);
            game.setWinnerSide(ETeam.MAFIA);
            gameRepository.save(game);
            //TODO: нарахувати бали
            return true;
        } else if (!checkIfAtLeastOneMafiaIsAlive(gameId)){
            game.setLastUpdate(LocalDateTime.now());
            game.setGameStatus(EGameStatus.WAS_COMPLETED);
            game.setWinnerSide(ETeam.PEACE);
            gameRepository.save(game);
            return true;
        } else {
            return false;
        }
    }

    private boolean checkIfAtLeastOneMafiaIsAlive(Long gameId) {
        List<GameStatistics> gameStatisticsList = gameStatisticsService.getGameStatisticsByGameId(gameId);
        gameStatisticsList = gameStatisticsList.stream()
                .filter(gameStatistics -> gameStatistics.getRole()!=null)
                .filter(GameStatistics::isInGame)
                .collect(Collectors.toList());
        return gameStatisticsList
                .stream().anyMatch(gs -> gs.getRole().getTeam().equals(ETeam.MAFIA));
    }

    private boolean checkIfMafiaAmountIsEqualsToPeaceAmount(Long gameId) {
        List<GameStatistics> gameStatisticsList = gameStatisticsService.getGameStatisticsByGameId(gameId);
        Map<ETeam, Long> eachTeamPlayersAmount = gameStatisticsList.stream()
                .filter(gameStatistics -> gameStatistics.getRole() != null)
                .filter(GameStatistics::isInGame)
                .collect(Collectors.groupingBy(
                        gameStatistics -> gameStatistics.getRole().getTeam(),
                        Collectors.counting()
                ));
        if (eachTeamPlayersAmount.size() > 0) {
            long mafiaPlayersAmount = eachTeamPlayersAmount.get(ETeam.MAFIA) != null ? eachTeamPlayersAmount.get(ETeam.MAFIA) : 0L;
            long peacePlayersAmount = eachTeamPlayersAmount.get(ETeam.PEACE) != null ? eachTeamPlayersAmount.get(ETeam.PEACE) : 0L;
            return mafiaPlayersAmount >= peacePlayersAmount;
        }else {
            return false;
        }
    }

    public Action doMafiaSelectionMove(long gameId, int playerToKillInGameNumber) {
        Action logger = new Action();
        logger.setActionText(" обрала гравця № " + playerToKillInGameNumber);
        logger.setLocalDateTime(LocalDateTime.now());
        return logger;
    }

    public Action doMafiaKillMove(long gameId, int playerToKillInGameNumber) {
        gameStatisticsService.killPlayer(gameId, playerToKillInGameNumber);
        Action logger = new Action();
        logger.setActionText("Мафія вистрілила у гравця № " + playerToKillInGameNumber);
        logger.setLocalDateTime(LocalDateTime.now());
        return logger;
    }

    public Action doDoctorMove(long gameId, int playerToHealInGameNumber) {
        gameStatisticsService.healPlayer(gameId, playerToHealInGameNumber);
        Action logger = new Action();
        logger.setActionText("Лікар лікує гравця № " + playerToHealInGameNumber);
        logger.setLocalDateTime(LocalDateTime.now());
        return logger;
    }

    public Action doLedyMove(Long currentGameId, int chosenPlayerNumber) {
        gameStatisticsService.blockVotingPerDay(currentGameId, chosenPlayerNumber);
        Action logger = new Action();
        logger.setActionText("Леді обирає гравця № " + chosenPlayerNumber);
        logger.setLocalDateTime(LocalDateTime.now());
        return logger;
    }

    public Action doManiakMove(Long currentGameId, int chosenPlayerNumber) {
        gameStatisticsService.killPlayer(currentGameId, chosenPlayerNumber);
        Action logger = new Action();
        logger.setActionText("Маніяк вистрілив у гравця № " + chosenPlayerNumber);
        logger.setLocalDateTime(LocalDateTime.now());
        return logger;
    }

    public Action doStrilochnykMove(Long currentGameId, int chosenPlayerNumber) {
        gameStatisticsService.killPlayer(currentGameId, chosenPlayerNumber);
        Action logger = new Action();
        logger.setActionText("Стрілочник вистрілив у гравця № " + chosenPlayerNumber);
        logger.setLocalDateTime(LocalDateTime.now());
        return logger;
    }

    public void resetStrilochnykAttempts(Long currentGameId) {
        for (GameStatistics gameStatistics: gameStatisticsService.getGameStatisticsByGameId(currentGameId)) {
            if (gameStatistics.getRole().getRoleNameConstant().equals(ERoleOrder.STRILOCHNYK.name())) {
                gameStatistics.setTimesWasKilled((short) 0);
                gameStatisticsRepository.save(gameStatistics);
            }
        }
    }
}
