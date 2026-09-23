package fr.m2.groupe7.kiestla.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
// TODO: remettre la contrainte d'unicité quand la relation module sera réactivée
// @Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "module_id"}))
@Table(name = "enrollments")
@Getter @Setter
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

        /*
    @ManyToOne(optional = false)
    @JoinColumn(name = "module_id")
    private Module module;
        */
}
