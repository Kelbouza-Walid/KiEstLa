package fr.m2.groupe7.kiestla;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.Comment;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Notif;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.CommentRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.NotifRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserDeleteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotifRepository notifRepository;

    private User admin;
    private User student;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        admin = createUser("System", "Admin", "admin", Role.ADMIN);
        student = createUser("John", "Doe", "jdoe", Role.STUDENT);

        User teacher = createUser("Alan", "Turing", "aturing", Role.TEACHER);

        Module module = new Module();
        module.setName("Algorithmique");
        module.setReferenceTeacher(teacher);
        module = moduleRepository.save(module);

        student.getModules().add(module);
        student = userRepository.save(student);

        Absence absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(LocalDate.of(2026, 10, 10));
        absence = absenceRepository.save(absence);

        Comment comment = new Comment();
        comment.setAbsence(absence);
        comment.setCommenter(student);
        comment.setDate(LocalDateTime.now());
        comment.setComment("Justification en attente");
        commentRepository.save(comment);

        Notif notif = new Notif();
        notif.setUser(student);
        notif.setStatus("UNREAD");
        notifRepository.save(notif);
    }

    @AfterEach
    void cleanDatabase() {
        commentRepository.deleteAll();
        notifRepository.deleteAll();
        absenceRepository.deleteAll();
        moduleRepository.findAll().forEach(m -> {
            m.setReferenceTeacher(null);
            moduleRepository.save(m);
        });
        userRepository.deleteAll();
        moduleRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminDeletesUserSuccessfully() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/delete", student.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("deleteSuccessMessage", "l'utilisateur john doe a bien été supprimer"));

        assertFalse(userRepository.existsById(student.getId()));
        assertTrue(absenceRepository.findByStudentId(student.getId()).isEmpty());
        assertTrue(commentRepository.findByCommenterId(student.getId()).isEmpty());
        assertTrue(notifRepository.findByUserId(student.getId()).isEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCannotDeleteThemselves() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/delete", admin.getId()).with(csrf()).header("Referer", "/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("errorMessage", "You cannot delete your own account."));

        assertTrue(userRepository.existsById(admin.getId()));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotDeleteUser() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/delete", student.getId()).with(csrf()))
                .andExpect(status().isForbidden());

        assertTrue(userRepository.existsById(student.getId()));
    }

    private User createUser(String firstName, String lastName, String login, Role role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLogin(login);
        user.setPassword("{noop}password");
        user.setRole(role);
        return userRepository.save(user);
    }
}
