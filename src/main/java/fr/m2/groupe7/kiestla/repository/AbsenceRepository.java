package fr.m2.groupe7.kiestla.repository;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbsenceRepository extends JpaRepository<Absence, Long> {
    List<Absence> findByStudentId(Long studentId);
    List<Absence> findByModuleId(Long moduleId);
    List<Absence> findByStatus(AbsenceStatus status);
    List<Absence> findByStudentIdAndModuleId(Long studentId, Long moduleId);
    List<Absence> findByStudentIdAndStatus(Long studentId, AbsenceStatus status);
}
