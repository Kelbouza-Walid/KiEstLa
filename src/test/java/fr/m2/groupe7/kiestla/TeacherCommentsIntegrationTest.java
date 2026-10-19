package fr.m2.groupe7.kiestla;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.CommentRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.NotifRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class TeacherCommentsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private NotifRepository notifRepository;

    @Autowired
    private CommentRepository commentRepository;

    private Module module;
    private Absence absence;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        User student = createUser("Gaspard", "Quentin", "gquentin", Role.STUDENT);
        User teacher = createUser("Fabrice", "Bouquet", "fbouquet", Role.TEACHER);
        createUser("Nicolas", "Marilleau", "nmarilleau", Role.TEACHER);
        createUser("Sarah", "Connor", "secretary1", Role.SECRETARY);

        module = new Module();
        module.setName("Test fonctionnel");
        module.setReferenceTeacher(teacher);
        module = moduleRepository.save(module);

        student.getModules().add(module);
        userRepository.save(student);
        teacher.getModules().add(module);
        userRepository.save(teacher);

        absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(LocalDate.now());
        absence = absenceRepository.save(absence);
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
    void teacherAbsencesPage_hasCommentsButtonAndPanel() throws Exception {
        mockMvc.perform(get("/teacher/absences").with(user("fbouquet").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-absence-id=\"" + absence.getId() + "\"")))
                .andExpect(content().string(containsString("data-testid=\"comments-panel\"")));
    }

    @Test
    void moduleDetailsPage_hasCommentsForTeacherButNotForAdmin() throws Exception {
        mockMvc.perform(get("/module-details/{id}", module.getId()).with(user("fbouquet").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"btn-see-comments\"")))
                .andExpect(content().string(containsString("data-testid=\"comments-panel\"")));

        mockMvc.perform(get("/module-details/{id}", module.getId()).with(user("admin1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("btn-see-comments"))))
                .andExpect(content().string(not(containsString("comments-panel"))));
    }

    @Test
    void teacherComment_isReadableByTheSecretary() throws Exception {
        mockMvc.perform(post("/absences/{id}/comments", absence.getId())
                .with(user("fbouquet").roles("TEACHER"))
                .with(csrf())
                .header("Referer", "/teacher/absences")
                .param("comment", "Was the student sick?"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/absences"))
                .andExpect(flash().attribute("openCommentsFor", absence.getId()));

        assertEquals(1, commentRepository.count());

        // The secretary sees the comment but cannot edit it
        mockMvc.perform(get("/absences/{id}/comments", absence.getId()).with(user("secretary1").roles("SECRETARY")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Was the student sick?")))
                .andExpect(content().string(not(containsString("btn-edit-comment"))));
    }

    @Test
    void teacherNotInTheModule_cannotSeeTheComments() throws Exception {
        mockMvc.perform(get("/absences/{id}/comments", absence.getId()).with(user("nmarilleau").roles("TEACHER")))
                .andExpect(status().isForbidden());
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
