package fr.m2.groupe7.kiestla;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AdminModuleDeleteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private Module module;
    private User student;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        User teacher = createUser("delete-teacher", Role.TEACHER);

        module = new Module();
        module.setName("Module à supprimer");
        module.setReferenceTeacher(teacher);
        module = moduleRepository.save(module);

        student = createUser("delete-student", Role.STUDENT);
        student.getModules().add(module);
        student = userRepository.save(student);

        Absence absence = new Absence();
        absence.setStudent(student);
        absence.setModule(module);
        absence.setDate(LocalDate.of(2026, 10, 1));
        absenceRepository.save(absence);
    }

    @AfterEach
    void cleanDatabase() {
        absenceRepository.deleteAll();
        // modules and users reference each other: drop the reference teachers first
        moduleRepository.findAll().forEach(m -> {
            m.setReferenceTeacher(null);
            moduleRepository.save(m);
        });
        userRepository.deleteAll();
        moduleRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDeletesModuleWithItsEnrollmentsAndAbsences() throws Exception {
        mockMvc.perform(post("/admin/modules/{id}/delete", module.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/modules"));

        assertFalse(moduleRepository.existsById(module.getId()));
        assertTrue(absenceRepository.findByModuleId(module.getId()).isEmpty());
        // users are kept, only their enrollment is removed
        assertEquals(2, userRepository.count());
        assertTrue(userRepository.findWithModulesById(student.getId()).orElseThrow().getModules().isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletingUnknownModuleDeletesNothing() throws Exception {
        mockMvc.perform(post("/admin/modules/{id}/delete", module.getId() + 1000).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertTrue(moduleRepository.existsById(module.getId()));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotDeleteModule() throws Exception {
        mockMvc.perform(post("/admin/modules/{id}/delete", module.getId()).with(csrf()))
                .andExpect(status().isForbidden());

        assertTrue(moduleRepository.existsById(module.getId()));
    }

    private User createUser(String login, Role role) {
        User user = new User();
        user.setFirstName("First");
        user.setLastName("Last");
        user.setLogin(login);
        user.setPassword("{noop}testpassword");
        user.setRole(role);
        return userRepository.save(user);
    }
}
