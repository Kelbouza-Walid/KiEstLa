package fr.m2.groupe7.kiestla.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifs")
@Getter @Setter
public class Notif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotifStatus status = NotifStatus.UNREAD;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "module_id")
    private Long moduleId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notif() {
    }

    public Notif(User user, String message) {
        this.user = user;
        this.message = message;
        this.status = NotifStatus.UNREAD;
        this.createdAt = LocalDateTime.now();
    }

    public Notif(User user, String message, Long studentId, Long moduleId) {
        this.user = user;
        this.message = message;
        this.studentId = studentId;
        this.moduleId = moduleId;
        this.status = NotifStatus.UNREAD;
        this.createdAt = LocalDateTime.now();
    }
}
