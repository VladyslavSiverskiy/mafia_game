package com.vsiverskyi.service;

import com.vsiverskyi.model.Role;
import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

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
}
