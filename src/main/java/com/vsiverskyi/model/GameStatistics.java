package com.vsiverskyi.model;

import jakarta.persistence.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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
    private boolean skipNextVoting;
    @Column
    private int yellowCards;
    @Column
    private byte redCards;
    @Column
    private short timesWasHealed = 0;
    /**
     * Для таких ролей, як Стрілочник
     */
    @Column
    private short timesWasKilled = 0;
    @Column
    private short excusesAttempts = 0;
    /**
     * Для Отамана
     * */
    @Column
    private boolean defendedPerNextVoting;
    /***
     * Для леді
     */
    @Column
    private boolean poisonedByLady;
    @Column
    private short nightsTillDeath;
    /**
     * Для бомби
     */
    @Column
    private boolean wasMarkedByBomb;
    /**
     * Для крадія
     * */
    @Column
    private boolean wasMarkedByKradiy;
    @Column
    private boolean headledOnThePreviousStage;

    public Circle getAvatarCircle() {
        Circle circle = new Circle(10, Color.DARKGREY); // Set the desired radius
        return circle;
    }

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
