package com.vsiverskyi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "game_stats")
public class GameStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;
    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
    @Column
    private Integer points;
    @Column
    private boolean inGame; // true якщо живий, false - ні
    @Column
    private String inGameNickname; // нікнейм в грі, ще може бути прикріплений nickname в Player
    @Column
    private Integer inGameNumber; // Номер гравця у грі
    @Column
    private Boolean skipNextVoting;
    @Column
    private int yellowCards;
    @Column
    private byte redCards;
    @Column
    private Short timesWasHealed;

    @Override
    public String toString() {
        return "GameStatistics{" +
               "id=" + id +
               ", points=" + points +
               ", inGame=" + inGame +
               ", inGameNickname='" + inGameNickname + '\'' +
               ", inGameNumber=" + inGameNumber +
               ", skipNextVoting=" + skipNextVoting +
               ", yellowCards=" + yellowCards +
               ", redCards=" + redCards +
               ", timesWasHealed=" + timesWasHealed +
               '}';
    }
}
