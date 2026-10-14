package fr.m2.groupe7.kiestla;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AdminModuleCreateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private User teacher;
    private User student;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        teacher = createUser("create-teacher", Role.TEACHER);
        student = createUser("create-student", Role.STUDENT);
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
    void adminCreatesModuleWithReferenceTeacher() throws Exception {
        mockMvc.perform(post("/admin/modules").with(csrf())
                .param("name", " Mathématiques ")
                .param("referenceTeacherId", teacher.getId().toString()))
                .andExpect(redirectedUrl("/admin/modules"))
                .andExpect(flash().attributeExists("successMessage"));

        Module created = moduleRepository.findByName("Mathématiques").orElseThrow();
        assertEquals(teacher.getId(), created.getReferenceTeacher().getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blankNameIsRejected() throws Exception {
        mockMvc.perform(post("/admin/modules").with(csrf())
                .header("Referer", "/admin/modules")
                .param("name", "   ")
                .param("referenceTeacherId", teacher.getId().toString()))
                .andExpect(redirectedUrl("/admin/modules"))
                .andExpect(flash().attribute("errorMessage", "The module name is required."));

        assertEquals(0, moduleRepository.count());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missingReferenceTeacherIsRejected() throws Exception {
        mockMvc.perform(post("/admin/modules").with(csrf())
                .header("Referer", "/admin/modules")
                .param("name", "Mathématiques"))
                .andExpect(redirectedUrl("/admin/modules"))
                .andExpect(flash().attribute("errorMessage", "The reference teacher is required."));

        assertEquals(0, moduleRepository.count());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateNameIsRejectedIgnoringCase() throws Exception {
        Module existing = new Module();
        existing.setName("Mathématiques");
        moduleRepository.save(existing);

        mockMvc.perform(post("/admin/modules").with(csrf())
                .header("Referer", "/admin/modules")
                .param("name", "mathématiques")
                .param("referenceTeacherId", teacher.getId().toString()))
                .andExpect(redirectedUrl("/admin/modules"))
                .andExpect(flash().attribute("errorMessage", "A module with this name already exists."));

        assertEquals(1, moduleRepository.count());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void referenceTeacherMustBeATeacher() throws Exception {
        mockMvc.perform(post("/admin/modules").with(csrf())
                .header("Referer", "/admin/modules")
                .param("name", "Mathématiques")
                .param("referenceTeacherId", student.getId().toString()))
                .andExpect(redirectedUrl("/admin/modules"))
                .andExpect(flash().attribute("errorMessage", "The selected user is not a teacher."));

        assertEquals(0, moduleRepository.count());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotCreateModule() throws Exception {
        mockMvc.perform(post("/admin/modules").with(csrf())
                .param("name", "Mathématiques")
                .param("referenceTeacherId", teacher.getId().toString()))
                .andExpect(status().isForbidden());

        assertEquals(0, moduleRepository.count());
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
