package com.vsiverskyi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "nicknames")
public class Nickname {

    @Id
    private long id;
    private String nickname;
}
