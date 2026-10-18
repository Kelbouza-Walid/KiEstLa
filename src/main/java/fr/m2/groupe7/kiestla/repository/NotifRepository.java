package fr.m2.groupe7.kiestla.repository;

import fr.m2.groupe7.kiestla.entity.Notif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotifRepository extends JpaRepository<Notif, Long> {
    List<Notif> findByUserId(Long userId);
    List<Notif> findByUserIdAndStatus(Long userId, String status);
    void deleteByUserId(Long userId);
}
