package fr.m2.groupe7.kiestla.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.m2.groupe7.kiestla.dto.ModuleSummary;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;

/**
 * Unit tests for the modules list of the student's home page (feature 931).
 */
@ExtendWith(MockitoExtension.class)
class ModuleServiceStudentModulesTest {

    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AbsenceRepository absenceRepository;
    @InjectMocks
    private ModuleService moduleService;

    @Test
    void getModulesForStudent_sortedByNameWithReferenceTeacher() {
        User teacherA = createUser(1L, "teacherA", Role.TEACHER);
        User teacherB = createUser(2L, "teacherB", Role.TEACHER);
        User student = createUser(3L, "student1", Role.STUDENT);
        student.getModules().add(createModule(20L, "M2", teacherB));
        student.getModules().add(createModule(10L, "M1", teacherA));
        when(userRepository.findModulesByLogin("student1")).thenReturn(Optional.of(student));

        List<ModuleSummary> result = moduleService.getModulesForStudent("student1");

        assertEquals(List.of(
                new ModuleSummary(10L, "M1", "teacherA"),
                new ModuleSummary(20L, "M2", "teacherB")), result);
    }

    @Test
    void getModulesForStudent_moduleWithoutReferenceTeacher() {
        User student = createUser(3L, "student1", Role.STUDENT);
        student.getModules().add(createModule(10L, "M1", null));
        when(userRepository.findModulesByLogin("student1")).thenReturn(Optional.of(student));

        List<ModuleSummary> result = moduleService.getModulesForStudent("student1");

        assertEquals(List.of(new ModuleSummary(10L, "M1", null)), result);
    }

    @Test
    void getModulesForStudent_noModules() {
        when(userRepository.findModulesByLogin("student1"))
                .thenReturn(Optional.of(createUser(3L, "student1", Role.STUDENT)));

        assertTrue(moduleService.getModulesForStudent("student1").isEmpty());
    }

    @Test
    void getModulesForStudent_unknownStudent() {
        when(userRepository.findModulesByLogin("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.getModulesForStudent("unknown"));
    }

    private Module createModule(Long id, String name, User reference) {
        Module module = new Module();
        module.setId(id);
        module.setName(name);
        module.setReferenceTeacher(reference);
        return module;
    }

    private User createUser(Long id, String name, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstName("First");
        user.setLastName(name);
        user.setLogin(name);
        user.setRole(role);
        return user;
    }
}
