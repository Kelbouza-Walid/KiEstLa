package fr.m2.groupe7.kiestla.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Entity
@Table(name = "modules")
@Getter
@Setter
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne
    @JoinColumn(name = "reference_teacher_id")
    private User referenceTeacher;

    @ManyToMany(mappedBy = "modules")
    private Set<User> enrolledUsers = new HashSet<>();

    public List<User> getEnrolledStudents() {
        return enrolledUsers.stream()
                .filter(u -> u.getRole() == Role.STUDENT)
                .toList();
    }

    public List<User> getAllTeachers() {
        return Stream.concat(
                enrolledUsers.stream().filter(u -> u.getRole() == Role.TEACHER),
                Stream.ofNullable(referenceTeacher)).distinct().toList();
    }

}
