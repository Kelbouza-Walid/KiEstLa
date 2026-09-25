package fr.m2.groupe7.kiestla.repository;

import fr.m2.groupe7.kiestla.entity.Absence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Absence entities.
 */
@Repository
public interface AbsenceRepository extends JpaRepository<Absence, Long> {
}
