package com.vsiverskyi.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(unique = true)
    private String username;
    @Column
    private String name;
    @Column
    private String surname;
    @Column
    private String city;
    @Column
    private Short age;
    @OneToMany(mappedBy = "player")
    private List<GameStatistics> statistics;

    @Override
    public String toString() {
        return "Player{" +
               "id=" + id +
               ", username='" + username + '\'' +
               ", name='" + name + '\'' +
               ", surname='" + surname + '\'' +
               ", city='" + city + '\'' +
               ", age=" + age +
               '}';
    }
}
