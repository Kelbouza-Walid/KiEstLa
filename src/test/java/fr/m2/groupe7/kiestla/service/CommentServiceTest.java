package fr.m2.groupe7.kiestla.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.m2.groupe7.kiestla.dto.CommentRequest;
import fr.m2.groupe7.kiestla.dto.CommentSummary;
import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.Comment;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.CommentRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CommentService.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final Long MODULE_ID = 10L;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AbsenceRepository absenceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModuleService moduleService;

    @InjectMocks
    private CommentService commentService;

    // ---------- getCommentsForAbsence ----------

    @Test
    void getCommentsForAbsence_secretary() {
        User secretary = mockUser(1L, "secretary", Role.SECRETARY);
        Absence absence = mockAbsence(1L);
        Comment comment = createComment(5L, absence, secretary, "Called the parents");
        when(commentRepository.findByAbsenceIdOrderByDateAsc(1L)).thenReturn(List.of(comment));

        List<CommentSummary> comments = commentService.getCommentsForAbsence(1L, "secretary");

        assertEquals(1, comments.size());
        assertEquals("Called the parents", comments.get(0).comment());
        assertEquals("secretary", comments.get(0).userLogin());
        verifyNoInteractions(moduleService);
    }

    @Test
    void getCommentsForAbsence_teacherOfTheModule() {
        mockUser(2L, "teacher", Role.TEACHER);
        mockAbsence(1L);
        when(commentRepository.findByAbsenceIdOrderByDateAsc(1L)).thenReturn(List.of());

        assertEquals(0, commentService.getCommentsForAbsence(1L, "teacher").size());
        verify(moduleService).verifyTeacherAccess(MODULE_ID, "teacher");
    }

    @Test
    void getCommentsForAbsence_teacherNotInTheModule() {
        mockUser(2L, "teacher", Role.TEACHER);
        mockAbsence(1L);
        when(moduleService.verifyTeacherAccess(MODULE_ID, "teacher"))
                .thenThrow(new BusinessException("Vous n'êtes pas autorisé à accéder à ce module."));

        assertThrows(BusinessException.class, () -> commentService.getCommentsForAbsence(1L, "teacher"));
        verify(commentRepository, never()).findByAbsenceIdOrderByDateAsc(any());
    }

    @Test
    void getCommentsForAbsence_student() {
        mockUser(3L, "student", Role.STUDENT);
        mockAbsence(1L);

        assertThrows(BusinessException.class, () -> commentService.getCommentsForAbsence(1L, "student"));
    }

    @Test
    void getCommentsForAbsence_absenceNotFound() {
        when(absenceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.getCommentsForAbsence(99L, "secretary"));
    }

    // ---------- addComment ----------

    @Test
    void addComment_basic() {
        User secretary = mockUser(1L, "secretary", Role.SECRETARY);
        Absence absence = mockAbsence(1L);

        commentService.addComment(1L, "secretary", new CommentRequest("Called the parents"));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertEquals(absence, captor.getValue().getAbsence());
        assertEquals(secretary, captor.getValue().getCommenter());
        assertEquals("Called the parents", captor.getValue().getComment());
        assertNotNull(captor.getValue().getDate());
    }

    @Test
    void addComment_teacherNotInTheModule() {
        mockUser(2L, "teacher", Role.TEACHER);
        mockAbsence(1L);
        when(moduleService.verifyTeacherAccess(MODULE_ID, "teacher"))
                .thenThrow(new BusinessException("Vous n'êtes pas autorisé à accéder à ce module."));

        assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "teacher", new CommentRequest("Text")));
        verify(commentRepository, never()).save(any());
    }

    // ---------- editComment / deleteComment ----------

    @Test
    void editComment_basic() {
        User secretary = mockUser(1L, "secretary", Role.SECRETARY);
        Comment comment = mockComment(5L, secretary, "Called the parents");

        Long absenceId = commentService.editComment(5L, "secretary", new CommentRequest("Called the parents twice"));

        assertEquals(1L, absenceId);
        assertEquals("Called the parents twice", comment.getComment());
    }

    @Test
    void editComment_notAuthor() {
        mockUser(1L, "secretary", Role.SECRETARY);
        Comment comment = mockComment(5L, createUser(2L, "other", Role.SECRETARY), "Called the parents");

        assertThrows(BusinessException.class,
                () -> commentService.editComment(5L, "secretary", new CommentRequest("Hacked")));
        assertEquals("Called the parents", comment.getComment());
    }

    @Test
    void editComment_authorNoLongerInTheModule() {
        User teacher = mockUser(2L, "teacher", Role.TEACHER);
        Comment comment = mockComment(5L, teacher, "Called the parents");
        when(moduleService.verifyTeacherAccess(MODULE_ID, "teacher"))
                .thenThrow(new BusinessException("Vous n'êtes pas autorisé à accéder à ce module."));

        assertThrows(BusinessException.class,
                () -> commentService.editComment(5L, "teacher", new CommentRequest("Hacked")));
        assertEquals("Called the parents", comment.getComment());
    }

    @Test
    void editComment_commentNotFound() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.editComment(99L, "secretary", new CommentRequest("Text")));
    }

    @Test
    void deleteComment_basic() {
        User secretary = mockUser(1L, "secretary", Role.SECRETARY);
        Comment comment = mockComment(5L, secretary, "Called the parents");

        Long absenceId = commentService.deleteComment(5L, "secretary");

        assertEquals(1L, absenceId);
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_notAuthor() {
        mockUser(1L, "secretary", Role.SECRETARY);
        mockComment(5L, createUser(2L, "other", Role.SECRETARY), "Called the parents");

        assertThrows(BusinessException.class, () -> commentService.deleteComment(5L, "secretary"));
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_commentNotFound() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.deleteComment(99L, "secretary"));
    }

    // ---------- helpers ----------

    private User createUser(Long id, String login, Role role) {
        User user = new User();
        user.setId(id);
        user.setLogin(login);
        user.setRole(role);
        return user;
    }

    private User mockUser(Long id, String login, Role role) {
        User user = createUser(id, login, role);
        when(userRepository.findByLogin(login)).thenReturn(Optional.of(user));
        return user;
    }

    private Absence createAbsence(Long id) {
        Module module = new Module();
        module.setId(MODULE_ID);

        Absence absence = new Absence();
        absence.setId(id);
        absence.setModule(module);
        return absence;
    }

    private Absence mockAbsence(Long id) {
        Absence absence = createAbsence(id);
        when(absenceRepository.findById(id)).thenReturn(Optional.of(absence));
        return absence;
    }

    private Comment createComment(Long id, Absence absence, User commenter, String text) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setAbsence(absence);
        comment.setCommenter(commenter);
        comment.setComment(text);
        return comment;
    }

    private Comment mockComment(Long id, User commenter, String text) {
        Comment comment = createComment(id, createAbsence(1L), commenter, text);
        when(commentRepository.findById(id)).thenReturn(Optional.of(comment));
        return comment;
    }
}
