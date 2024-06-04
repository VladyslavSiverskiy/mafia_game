package com.vsiverskyi.model;

import com.vsiverskyi.model.enums.ETeam;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private String title;

    @Column
    @Enumerated(EnumType.STRING)
    private ETeam team;

    @Override
    public String toString() {
        return "Role{" +
               "id=" + id +
               ", title='" + title + '\'' +
               ", team=" + team +
               '}';
    }
}
