package com.vsiverskyi.model;

import com.vsiverskyi.model.enums.EGameStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;

    @Column
    @Enumerated(EnumType.STRING)
    private EGameStatus gameStatus;

    @Column
    private Integer playersAmount;

    @OneToMany(mappedBy = "game",fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<GameStatistics> gameStatistics;

    @Override
    public String toString() {
        return "Game{" +
               "id=" + id +
               ", lastUpdate=" + lastUpdate +
               ", gameStatus=" + gameStatus +
               ", playersAmount=" + playersAmount +
               ", gameStatistics=" + gameStatistics +
               '}';
    }
}
