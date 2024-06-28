package com.vsiverskyi.model;

import com.vsiverskyi.model.enums.ERoleOrder;
import com.vsiverskyi.model.enums.ETeam;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

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
    @Column
    private String roleNameConstant;

    @Override
    public String toString() {
        return "Role{" +
               "id=" + id +
               ", title='" + title + '\'' +
               ", team=" + team +
               ", roleNameConstant=" + roleNameConstant +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
