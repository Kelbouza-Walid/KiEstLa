package fr.m2.groupe7.kiestla;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

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

/**
 * Tests for the module members management of the administrator (TFKIES26-108).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminModuleMembersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    private Module module;
    private User reference;
    private User enrolledTeacher;
    private User otherTeacher;
    private User enrolledStudent;
    private User otherStudent;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        reference = createUser("members-reference", Role.TEACHER);
        enrolledTeacher = createUser("members-teacher1", Role.TEACHER);
        otherTeacher = createUser("members-teacher2", Role.TEACHER);
        enrolledStudent = createUser("members-student1", Role.STUDENT);
        otherStudent = createUser("members-student2", Role.STUDENT);

        module = new Module();
        module.setName("Module membres");
        module.setReferenceTeacher(reference);
        module = moduleRepository.save(module);

        enrolledTeacher.getModules().add(module);
        enrolledTeacher = userRepository.save(enrolledTeacher);
        enrolledStudent.getModules().add(module);
        enrolledStudent = userRepository.save(enrolledStudent);
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
    void membersPageShowsEveryTeacherAndStudent() throws Exception {
        mockMvc.perform(get("/admin/modules/{id}/members", module.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-module-members"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("checkbox-teacher-members-teacher2")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("checkbox-student-members-student2")))
                // the reference teacher is managed separately
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("checkbox-teacher-members-reference"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEnrollsAndUnenrollsMembers() throws Exception {
        mockMvc.perform(post("/admin/modules/{id}/members", module.getId()).with(csrf())
                .param("referenceTeacherId", reference.getId().toString())
                .param("teacherIds", otherTeacher.getId().toString())
                .param("studentIds", enrolledStudent.getId().toString(), otherStudent.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/modules/" + module.getId() + "/members"))
                .andExpect(flash().attributeExists("successMessage"));

        assertEquals(Set.of("members-teacher2", "members-student1", "members-student2"), memberLogins());
        // the reference teacher is still the reference teacher
        assertEquals(reference.getId(), moduleRepository.findById(module.getId()).orElseThrow()
                .getReferenceTeacher().getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void nothingSelectedUnenrollsEveryone() throws Exception {
        mockMvc.perform(post("/admin/modules/{id}/members", module.getId()).with(csrf())
                .param("referenceTeacherId", reference.getId().toString()))
                .andExpect(status().is3xxRedirection());

        assertTrue(memberLogins().isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unenrolledStudentKeepsAbsences() throws Exception {
        Absence absence = new Absence();
        absence.setStudent(enrolledStudent);
        absence.setModule(module);
        absence.setDate(LocalDate.of(2026, 10, 1));
        absenceRepository.save(absence);

        mockMvc.perform(post("/admin/modules/{id}/members", module.getId()).with(csrf())
                .param("referenceTeacherId", reference.getId().toString())
                .param("teacherIds", enrolledTeacher.getId().toString()))
                .andExpect(status().is3xxRedirection());

        assertEquals(Set.of("members-teacher1"), memberLogins());
        assertEquals(1, absenceRepository.findByStudentId(enrolledStudent.getId()).size());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void studentCannotBeEnrolledAsTeacher() throws Exception {
        mockMvc.perform(post("/admin/modules/{id}/members", module.getId()).with(csrf())
                .header("Referer", "/admin/modules/" + module.getId() + "/members")
                .param("referenceTeacherId", reference.getId().toString())
                .param("teacherIds", otherStudent.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "L'utilisateur sélectionné n'est pas un professeur."));

        assertEquals(Set.of("members-teacher1", "members-student1"), memberLogins());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminChangesReferenceTeacher() throws Exception {
        mockMvc.perform(post("/admin/modules/{id}/members", module.getId()).with(csrf())
                .param("referenceTeacherId", otherTeacher.getId().toString())
                .param("teacherIds", enrolledTeacher.getId().toString(), reference.getId().toString())
                .param("studentIds", enrolledStudent.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));

        assertEquals(otherTeacher.getId(), moduleRepository.findById(module.getId()).orElseThrow()
                .getReferenceTeacher().getId());
        // the old reference teacher, checked, is now a regular teacher of the module
        assertEquals(Set.of("members-teacher1", "members-reference", "members-student1"), memberLogins());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void referenceTeacherIsRequired() throws Exception {
        mockMvc.perform(post("/admin/modules/{id}/members", module.getId()).with(csrf())
                .header("Referer", "/admin/modules/" + module.getId() + "/members")
                .param("teacherIds", enrolledTeacher.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "L'enseignant référent est obligatoire."));

        assertEquals(reference.getId(), moduleRepository.findById(module.getId()).orElseThrow()
                .getReferenceTeacher().getId());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotManageMembers() throws Exception {
        mockMvc.perform(post("/admin/modules/{id}/members", module.getId()).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/modules/{id}/members", module.getId()))
                .andExpect(status().isForbidden());

        assertEquals(Set.of("members-teacher1", "members-student1"), memberLogins());
    }

    private Set<String> memberLogins() {
        return userRepository.findAll().stream()
                .map(user -> userRepository.findWithModulesById(user.getId()).orElseThrow())
                .filter(user -> user.getModules().stream().anyMatch(m -> m.getId().equals(module.getId())))
                .map(User::getLogin)
                .collect(Collectors.toSet());
    }

    private User createUser(String login, Role role) {
        User user = new User();
        user.setFirstName("First");
        user.setLastName(login);
        user.setLogin(login);
        user.setPassword("{noop}testpassword");
        user.setRole(role);
        return userRepository.save(user);
    }
}
