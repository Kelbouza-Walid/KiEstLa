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

        String expectedMessage = "John Doe has more than 3 unexcused absences in the module Functional Testing";
        assertEquals(expectedMessage, teacherNotifs.get(0).getMessage());
        assertEquals(NotifStatus.UNREAD, teacherNotifs.get(0).getStatus());

        assertEquals(expectedMessage, secretaryNotifs.get(0).getMessage());
        assertEquals(NotifStatus.UNREAD, secretaryNotifs.get(0).getStatus());
    }

    @Test
    @WithMockUser(username = "secretary1", roles = "SECRETARY")
    void justifyingAbsence_whenCountDropsBelow3_removesNotifications() throws Exception {
        createAbsence(student, module, LocalDate.now().minusDays(3));
        createAbsence(student, module, LocalDate.now().minusDays(2));
        Absence third = createAbsence(student, module, LocalDate.now().minusDays(1));

        // Create notification as would happen when 3rd absence is recorded
        notifRepository.save(new Notif(teacherInModule, "notif", student.getId(), module.getId()));
        notifRepository.save(new Notif(secretary, "notif", student.getId(), module.getId()));

        assertEquals(2, notifRepository.count());

        // Secretary justifies the 3rd absence
        mockMvc.perform(post("/secretary/absence/{id}/justify", third.getId())
                .with(csrf())
                .param("motiveTitle", "Medical")
                .param("motiveDescription", "Doctor appointment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/secretary"));

        // Unjustified absence count is now 2 (< 3) -> notifications must be removed!
        assertEquals(0, notifRepository.count());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void teacherPage_displaysNotificationBellWithButtons() throws Exception {
        Notif notif = new Notif(teacherInModule, "John Doe has more than 3 unexcused absences in the module Functional Testing");
        notifRepository.save(notif);

        mockMvc.perform(get("/teacher"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher"))
                .andExpect(model().attribute("unreadNotificationsCount", 1L))
                .andExpect(content().string(containsString("btn-notifications")))
                .andExpect(content().string(containsString("notification-badge")))
                .andExpect(content().string(containsString("btn-mark-all-read")))
                .andExpect(content().string(containsString("btn-mark-read")))
                .andExpect(content().string(containsString("John Doe has more than 3 unexcused absences in the module Functional Testing")));
    }

    @Test
    @WithMockUser(username = "teacher1", roles = "TEACHER")
    void markNotificationAsRead_updatesStatusToRead() throws Exception {
        Notif notif = new Notif(teacherInModule, "John Doe has more than 3 unexcused absences in the module Functional Testing");
        notif = notifRepository.save(notif);

        assertEquals(NotifStatus.UNREAD, notif.getStatus());

        mockMvc.perform(post("/teacher/notifications/{id}/read", notif.getId())
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Notif updated = notifRepository.findById(notif.getId()).orElseThrow();
        assertEquals(NotifStatus.READ, updated.getStatus());
    }

    @Test
    @WithMockUser(username = "secretary1", roles = "SECRETARY")
    void secretaryPage_displaysNotificationBellWithButtons() throws Exception {
        Notif notif = new Notif(secretary, "John Doe has more than 3 unexcused absences in the module Functional Testing");
        notif = notifRepository.save(notif);

        mockMvc.perform(get("/secretary"))
                .andExpect(status().isOk())
                .andExpect(view().name("secretary"))
                .andExpect(model().attribute("unreadNotificationsCount", 1L))
                .andExpect(content().string(containsString("btn-notifications")))
                .andExpect(content().string(containsString("notification-badge")))
                .andExpect(content().string(containsString("btn-mark-all-read")))
                .andExpect(content().string(containsString("btn-mark-read")))
                .andExpect(content().string(containsString("John Doe has more than 3 unexcused absences in the module Functional Testing")));

        // Mark all as read
        mockMvc.perform(post("/secretary/notifications/read-all")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Notif updated = notifRepository.findById(notif.getId()).orElseThrow();
        assertEquals(NotifStatus.READ, updated.getStatus());
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
