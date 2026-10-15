package fr.m2.groupe7.kiestla.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.m2.groupe7.kiestla.dto.CommentRequest;
import fr.m2.groupe7.kiestla.dto.CommentSummary;
import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.Comment;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.CommentRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

/**
 * Comments on absences.
 * A secretary can access the comments of every absence, a teacher only those of the absences of their modules.
 * A comment can only be edited or deleted by its author.
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final AbsenceRepository absenceRepository;
    private final UserRepository userRepository;
    private final ModuleService moduleService;

    public CommentService(CommentRepository commentRepository,
            AbsenceRepository absenceRepository,
            UserRepository userRepository,
            ModuleService moduleService) {
        this.commentRepository = commentRepository;
        this.absenceRepository = absenceRepository;
        this.userRepository = userRepository;
        this.moduleService = moduleService;
    }

    /**
     * Retrieves all the comments of an absence, the oldest one first.
     *
     * @throws ResourceNotFoundException if the absence or the user doesn't exist
     * @throws BusinessException         if the user has no access to the absence
     */
    public List<CommentSummary> getCommentsForAbsence(Long absenceId, String login) {
        verifyAccess(getAbsence(absenceId), getUser(login));
        return commentRepository.findByAbsenceIdOrderByDateAsc(absenceId)
                .stream()
                .map(CommentSummary::from)
                .toList();
    }

    /**
     * Adds a comment written by the given user to an absence.
     *
     * @throws ResourceNotFoundException if the absence or the user doesn't exist
     * @throws BusinessException         if the user has no access to the absence
     */
    @Transactional
    public void addComment(Long absenceId, String commenterLogin, CommentRequest commentReq) {
        Absence absence = getAbsence(absenceId);
        User commenter = getUser(commenterLogin);
        verifyAccess(absence, commenter);

        Comment comment = new Comment();
        comment.setAbsence(absence);
        comment.setCommenter(commenter);
        comment.setDate(LocalDateTime.now());
        comment.setComment(commentReq.comment());
        commentRepository.save(comment);
    }

    /**
     * Replaces the text of a comment.
     *
     * @return the id of the commented absence
     * @throws ResourceNotFoundException if the comment or the user doesn't exist
     * @throws BusinessException         if the user is not the author of the comment
     */
    @Transactional
    public Long editComment(Long commentId, String editorLogin, CommentRequest commentReq) {
        Comment comment = getOwnComment(commentId, editorLogin);
        comment.setComment(commentReq.comment());
        return comment.getAbsence().getId();
    }

    /**
     * Deletes a comment.
     *
     * @return the id of the commented absence
     * @throws ResourceNotFoundException if the comment or the user doesn't exist
     * @throws BusinessException         if the user is not the author of the comment
     */
    @Transactional
    public Long deleteComment(Long commentId, String deleterLogin) {
        Comment comment = getOwnComment(commentId, deleterLogin);
        commentRepository.delete(comment);
        return comment.getAbsence().getId();
    }

    /**
     * @throws ResourceNotFoundException if the comment doesn't exist
     */
    public Long getAbsenceIdOfComment(Long commentId) {
        return getComment(commentId).getAbsence().getId();
    }

    private Comment getOwnComment(Long commentId, String login) {
        Comment comment = getComment(commentId);
        User user = getUser(login);
        if (!comment.getCommenter().getId().equals(user.getId())) {
            throw new BusinessException("Vous ne pouvez modifier que vos propres commentaires.");
        }
        // The author may have lost access to the absence since (e.g. teacher removed from the module)
        verifyAccess(comment.getAbsence(), user);
        return comment;
    }

    private void verifyAccess(Absence absence, User user) {
        if (user.getRole() == Role.SECRETARY) {
            return;
        }
        if (user.getRole() == Role.TEACHER) {
            moduleService.verifyTeacherAccess(absence.getModule().getId(), user.getLogin());
            return;
        }
        throw new BusinessException("Vous n'êtes pas autorisé à accéder aux commentaires.");
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire introuvable avec l'ID : " + commentId));
    }

    private Absence getAbsence(Long absenceId) {
        return absenceRepository.findById(absenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Absence introuvable avec l'ID : " + absenceId));
    }

    private User getUser(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + login));
    }
}
