package fr.m2.groupe7.kiestla.repository;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Absence entities.
 */
@Repository
public interface AbsenceRepository extends JpaRepository<Absence, Long> {
	List<Absence> findByStudentId(Long studentId);

	boolean existsByStudentId(Long studentId);

	long countByStudentId(Long studentId);

	List<Absence> findByModuleId(Long moduleId);

	@EntityGraph(attributePaths = "student")
	List<Absence> findByModuleIdOrderByDateDesc(Long moduleId);

	List<Absence> findByStatus(AbsenceStatus status);

	List<Absence> findByStudentIdAndModuleId(Long studentId, Long moduleId);

	List<Absence> findByStudentIdAndStatus(Long studentId, AbsenceStatus status);

	@EntityGraph(attributePaths = "student")
	List<Absence> findAllByOrderByDateDesc();

    @EntityGraph(attributePaths = "module")
    List<Absence> findByStudentLoginOrderByDateDesc(String login);

    @EntityGraph(attributePaths = "module")
    Optional<Absence> findByIdAndStudentLogin(Long id, String login);

    void deleteByModuleId(Long moduleId);

    void deleteByStudentId(Long studentId);
}
