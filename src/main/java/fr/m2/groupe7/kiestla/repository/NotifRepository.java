package fr.m2.groupe7.kiestla.repository;

import fr.m2.groupe7.kiestla.entity.Notif;
import fr.m2.groupe7.kiestla.entity.NotifStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotifRepository extends JpaRepository<Notif, Long> {
    List<Notif> findByUserId(Long userId);
    List<Notif> findByUserIdAndStatus(Long userId, String status);
    List<Notif> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notif> findByUserIdAndStatus(Long userId, NotifStatus status);
    long countByUserIdAndStatus(Long userId, NotifStatus status);
    List<Notif> findByStudentIdAndModuleId(Long studentId, Long moduleId);
    List<Notif> findByStudentId(Long studentId);
    boolean existsByUserIdAndStudentIdAndModuleIdAndStatus(Long userId, Long studentId, Long moduleId, NotifStatus status);
    void deleteByUserId(Long userId);
}
