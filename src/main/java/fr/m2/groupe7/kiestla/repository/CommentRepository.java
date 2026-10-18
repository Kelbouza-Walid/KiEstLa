package fr.m2.groupe7.kiestla.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.m2.groupe7.kiestla.entity.Comment;

/**
 * CommentRepository
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
	@EntityGraph(attributePaths = "commenter")
	List<Comment> findByAbsenceIdOrderByDateAsc(Long absenceId);

	List<Comment> findByCommenterId(Long commenterId);

	void deleteByCommenterId(Long commenterId);
}
