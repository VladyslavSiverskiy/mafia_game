package com.vsiverskyi.repository;

import com.vsiverskyi.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Role findByRoleNameConstant(String name);
    Optional<Role> findByTitle(String title);
}
