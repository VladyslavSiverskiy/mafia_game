package com.vsiverskyi.service;

import com.vsiverskyi.exception.CantStartGameException;
import com.vsiverskyi.exception.ExceptionConstants;
import com.vsiverskyi.exception.NoGameWithSuchIdException;
import com.vsiverskyi.exception.NoRoleWithSuchIdException;
import com.vsiverskyi.model.GameStatistics;
import com.vsiverskyi.model.Role;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.repository.GameStatisticsRepository;
import com.vsiverskyi.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private GameStatisticsRepository gameStatisticsRepository;

    @PostConstruct
    public void initRoles() {
        List<ERoleOrder> roles = Arrays.asList(ERoleOrder.values());
        for (ERoleOrder roleFromEnum : roles) {
            if (roleRepository.findByRoleNameConstant(roleFromEnum.name()) == null) {
                Role role = Role.builder()
                        .title(roleFromEnum.getTitle())
                        .roleNameConstant(roleFromEnum.name())
                        .build();
                roleRepository.save(role);
            }
        }
    }

    public void applyRoles(Long gameId, Map<Integer, Role> gamerIdRoles) throws RuntimeException {
        if(gameId == null) {
            throw new NoGameWithSuchIdException(ExceptionConstants.NO_GAME_WITH_SUCH_ID + gameId);
        }
        gamerIdRoles.forEach((gamerId, role) -> {
            if (role == null) {
                throw new CantStartGameException("Не всі ролі було розподілено, перезавантажте гру!");
            } else {
                Role role1 = roleRepository.findById(role.getId()).get();
                System.out.println(role1);
               GameStatistics gameStatistics = gameStatisticsRepository.findByGame_IdAndAndInGameNumber(gameId, gamerId);
               gameStatistics.setRole(role);
               GameStatistics gm = gameStatisticsRepository.save(gameStatistics);
                System.out.println(gm);
            }
        });
    }

    public List<Role> getRoleListFromListOfRoleId(List<Integer> roleIdList) {
        List<Role> roles = new ArrayList<>();
        for (Integer roleId : roleIdList) {
            Role role = roleRepository
                    .findById(roleId)
                    .orElseThrow(() -> new NoRoleWithSuchIdException("No role with ID: " + roleId));
            roles.add(role);
        }
        return roles;
    }
}
