package com.vsiverskyi.repository;


import com.vsiverskyi.model.Nickname;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NicknameRepository extends JpaRepository<Nickname, Long> {
}
