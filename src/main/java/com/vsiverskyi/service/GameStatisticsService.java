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

    public GameStatistics setInGameNickname() {
        return null;
    }

    public List<GameStatistics> getGameStatisticsByGameId(Long currentGameId) throws NoGameWithSuchIdException {
        Game game = gameRepository.findById(currentGameId).orElseThrow(() ->
                new NoGameWithSuchIdException(ExceptionConstants.NO_GAME_WITH_SUCH_ID + currentGameId));
        return game.getGameStatistics().stream()
                .sorted(Comparator.comparing(gs -> ERoleOrder.fromName(gs.getRole().getRoleNameConstant())))
                .collect(Collectors.toList());
    }
}
