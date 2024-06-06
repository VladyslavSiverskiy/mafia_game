package com.vsiverskyi.service;

import com.vsiverskyi.exception.CantStartGameException;
import com.vsiverskyi.exception.ExceptionConstants;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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


    /**
     * @return List of roles id
     */
    public List<Integer> initRolesPerGame(
            Integer playersAmount, Integer mafiaAmount, Map<String, Boolean> additionalRoles, Game game
    ) throws CantStartGameException {
        List<String> peaceRolesToAdd = additionalRoles.entrySet().stream()
                .filter(Map.Entry::getValue) // Filter entries with true values
                .map(Map.Entry::getKey) // Map to keys
                .collect(Collectors.toList()); // Collect to list
        if (playersAmount < mafiaAmount + peaceRolesToAdd.size()) {
            throw new CantStartGameException(ExceptionConstants.TOTAL_AMOUNT_OF_PLAYERS_IS_LOWER);
        }
        if (mafiaAmount >= playersAmount - mafiaAmount) {
            throw new CantStartGameException(ExceptionConstants.MAFIA_AMOUNT_IS_HIGHER_THAN_PEACE);
        }

        List<Integer> rolesIdToReturnList = new ArrayList<>();
        rolesIdToReturnList.addAll(returnPeaceSidePlayersId(playersAmount - mafiaAmount, peaceRolesToAdd, game));
        rolesIdToReturnList.addAll(returnMafiaSidePlayersId(mafiaAmount, game));
        return rolesIdToReturnList;
    }
    private List<Integer> returnPeaceSidePlayersId(int peaceAmount, List<String> additionalPeaceRoles, Game game) {
        List<Integer> peaceRolesIdList = new ArrayList<>();
        int amountOfPeacePlayers = peaceAmount;
        //create additional peace roles
        for (String roleTitle : additionalPeaceRoles) {
            Role peaceRole =
                    roleRepository
                            .findByTitle(roleTitle)
                            .orElseThrow(() ->
                                    new NoRoleWithSuchTitleException(ExceptionConstants.NO_ROLE_WITH_SUCH_TITLE + roleTitle));
            GameStatistics peacePlayer = GameStatistics.builder()
                    .game(game)
                    .inGame(true)
                    .points(0)
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
                    .points(0)
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
                .inGame(true)
                .build();
        gameStatisticsRepository.save(donPlayer);
        mafiaRoleIdList.add(donRole.getId());
        for (int i = 0; i < mafiaAmount - 1; i++) { // - 1 бо дона забираємо
            GameStatistics mafiaPlayer = GameStatistics.builder()
                    .game(game)
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
}
