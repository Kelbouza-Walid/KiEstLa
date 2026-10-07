package fr.m2.groupe7.kiestla.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.m2.groupe7.kiestla.dto.ModuleDetails;
import fr.m2.groupe7.kiestla.dto.UserSummary;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getAvailableStudentsForModule_basic() {
        UserSummary enrolledStudentSummary = new UserSummary(1L, "Noam", "Faivre", "nfaivre");
        ModuleDetails moduleDetails = new ModuleDetails(
                10L,
                "Test fonctionnel",
                "Fabrice Bouquet",
                List.of(enrolledStudentSummary),
                List.of());

        User enrolledStudent = createUser(1L, "Noam", "Faivre", "nfaivre", Role.STUDENT);
        User availableStudent = createUser(2L, "Suleyman", "Kacmaz", "skacmaz", Role.STUDENT);

        when(userRepository.findByRole(Role.STUDENT)).thenReturn(List.of(enrolledStudent, availableStudent));

        List<UserSummary> result = userService.getAvailableStudentsForModule(moduleDetails);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).id());
        assertEquals("Suleyman", result.get(0).firstName());
        assertEquals("Kacmaz", result.get(0).lastName());
        verify(userRepository).findByRole(Role.STUDENT);
    }

    @Test
    void getAvailableStudentsForModule_noMoreAvailableStudents() {
        UserSummary enrolled = new UserSummary(1L, "Mathis", "Regazzoni", "mregazzoni");
        ModuleDetails moduleDetails = new ModuleDetails(
                10L,
                "Architecture Multi-tiers",
                null,
                List.of(enrolled),
                List.of());

        User enrolledStudent = createUser(1L, "Mathis", "Regazzoni", "mregazzoni", Role.STUDENT);
        when(userRepository.findByRole(Role.STUDENT)).thenReturn(List.of(enrolledStudent));

        List<UserSummary> result = userService.getAvailableStudentsForModule(moduleDetails);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAvailableTeachersForModule_basic() {
        UserSummary enrolledTeacherSummary = new UserSummary(10L, "Fabrice", "Bouquet", "fbouquet");
        ModuleDetails moduleDetails = new ModuleDetails(
                10L,
                "Projet Ingénierie logicielle avancé",
                null,
                List.of(),
                List.of(enrolledTeacherSummary));

        User enrolledTeacher = createUser(10L, "Fabrice", "Bouquet", "fbouquet", Role.TEACHER);
        User availableTeacher = createUser(20L, "Nicolas", "Marilleau", "nmarilleau", Role.TEACHER);

        when(userRepository.findByRole(Role.TEACHER)).thenReturn(List.of(enrolledTeacher, availableTeacher));

        List<UserSummary> result = userService.getAvailableTeachersForModule(moduleDetails);

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).id());
        assertEquals("Nicolas", result.get(0).firstName());
        assertEquals("Marilleau", result.get(0).lastName());
        verify(userRepository).findByRole(Role.TEACHER);
    }

    private User createUser(Long id, String firstName, String lastName, String login, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLogin(login);
        user.setRole(role);
        return user;
    }
}
