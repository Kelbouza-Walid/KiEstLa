package fr.m2.groupe7.kiestla;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Notif;
import fr.m2.groupe7.kiestla.entity.NotifStatus;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.CommentRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.NotifRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AbsenceNotificationIntegrationTest {

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

    private User student;
    private User teacherInModule;
    private User teacherNotInModule;
    private User secretary;
    private Module module;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        student = createUser("John", "Doe", "student1", Role.STUDENT);
        teacherInModule = createUser("Alan", "Turing", "teacher1", Role.TEACHER);
        teacherNotInModule = createUser("Ada", "Lovelace", "teacher2", Role.TEACHER);
        secretary = createUser("Sarah", "Connor", "secretary1", Role.SECRETARY);

        module = new Module();
        module.setName("Functional Testing");
        module.setReferenceTeacher(teacherInModule);
        module = moduleRepository.save(module);

        student.getModules().add(module);
        userRepository.save(student);

        teacherInModule.getModules().add(module);
        userRepository.save(teacherInModule);
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
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void recordingThirdAbsence_triggersNotificationToModuleTeacherAndSecretary() throws Exception {
        // Enregistrer 2 absences injustifiées d'abord
        createAbsence(student, module, LocalDate.now().minusDays(3));
        createAbsence(student, module, LocalDate.now().minusDays(2));

        assertEquals(0, notifRepository.count());

        // Le professeur enregistre la 3ème absence
        mockMvc.perform(post("/teacher/modules/{id}/absences", module.getId())
                .with(csrf())
                .param("absentStudentId", student.getId().toString())
                .param("date", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/module-details/" + module.getId()));

        assertEquals(3, absenceRepository.countByStudentIdAndModuleIdAndStatus(student.getId(), module.getId(), fr.m2.groupe7.kiestla.entity.AbsenceStatus.UNJUSTIFIED));

        // Une notification a été envoyée au prof du module et à la secrétaire, pas au prof externe
        List<Notif> teacherNotifs = notifRepository.findByUserId(teacherInModule.getId());
        List<Notif> secretaryNotifs = notifRepository.findByUserId(secretary.getId());
        List<Notif> otherTeacherNotifs = notifRepository.findByUserId(teacherNotInModule.getId());

        assertEquals(1, teacherNotifs.size());
        assertEquals(1, secretaryNotifs.size());
        assertEquals(0, otherTeacherNotifs.size());

        String expectedMessage = "John Doe a plus de 3 absence injustifié dans le module Functional Testing";
        assertEquals(expectedMessage, teacherNotifs.get(0).getMessage());
        assertEquals(NotifStatus.UNREAD, teacherNotifs.get(0).getStatus());

        assertEquals(expectedMessage, secretaryNotifs.get(0).getMessage());
        assertEquals(NotifStatus.UNREAD, secretaryNotifs.get(0).getStatus());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void teacherPage_displaysNotificationBellWithRedBadgeAndMessage() throws Exception {
        Notif notif = new Notif(teacherInModule, "John Doe a plus de 3 absence injustifié dans le module Functional Testing");
        notifRepository.save(notif);

        mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher"))
                .andExpect(model().attribute("unreadNotificationsCount", 1L))
                .andExpect(content().string(containsString("btn-notifications")))
                .andExpect(content().string(containsString("notification-badge")))
                .andExpect(content().string(containsString("John Doe a plus de 3 absence injustifié dans le module Functional Testing")));
    }

    @Test
    @WithMockUser(username = "secretary1", roles = "SECRETARY")
    void secretaryPage_displaysNotificationBellWithRedBadgeAndMessage() throws Exception {
        Notif notif = new Notif(secretary, "John Doe a plus de 3 absence injustifié dans le module Functional Testing");
        notifRepository.save(notif);

        mockMvc.perform(get("/secretary"))
                .andExpect(status().isOk())
                .andExpect(view().name("secretary"))
                .andExpect(model().attribute("unreadNotificationsCount", 1L))
                .andExpect(content().string(containsString("btn-notifications")))
                .andExpect(content().string(containsString("notification-badge")))
                .andExpect(content().string(containsString("John Doe a plus de 3 absence injustifié dans le module Functional Testing")));
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

    private Absence createAbsence(User student, Module module, LocalDate date) {
        Absence absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(date);
        return absenceRepository.save(absence);
    }
}
