package fr.m2.groupe7.kiestla.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import fr.m2.groupe7.kiestla.dto.ModuleDetails;
import fr.m2.groupe7.kiestla.dto.AdminUserSummary;
import fr.m2.groupe7.kiestla.dto.CreateUserRequest;
import fr.m2.groupe7.kiestla.dto.CreatedUser;
import fr.m2.groupe7.kiestla.dto.UserSummary;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
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

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordGenerator passwordGenerator;

    @InjectMocks
    private UserService userService;

    @Test
    void getAllUsers() {
        User admin = createUser(1L, "System", "Admin", "admin", Role.ADMIN);
        admin.setPassword("hashed-password");
        when(userRepository.findAll()).thenReturn(List.of(admin));

        List<AdminUserSummary> result = userService.getAllUsers();

        assertEquals(List.of(new AdminUserSummary(1L, "System", "Admin", "admin", Role.ADMIN)), result);
        verify(userRepository).findAll();
    }

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

    @Test
    void createUser_success() {
        CreateUserRequest request = new CreateUserRequest(" Fabrice ", " Bouquet ", " fbouquet ", Role.TEACHER);
        when(userRepository.existsByLogin("fbouquet")).thenReturn(false);
        when(passwordGenerator.generate()).thenReturn("Abc23xyz45Kq");
        when(passwordEncoder.encode("Abc23xyz45Kq")).thenReturn("{bcrypt}hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        CreatedUser result = userService.createUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Fabrice", saved.getFirstName());
        assertEquals("Bouquet", saved.getLastName());
        assertEquals("fbouquet", saved.getLogin());
        assertEquals(Role.TEACHER, saved.getRole());
        assertEquals("{bcrypt}hashed", saved.getPassword());

        assertEquals(new AdminUserSummary(42L, "Fabrice", "Bouquet", "fbouquet", Role.TEACHER), result.user());
        assertEquals("Abc23xyz45Kq", result.generatedPassword());
    }

    @Test
    void createUser_duplicateLogin() {
        CreateUserRequest request = new CreateUserRequest("Fabrice", "Bouquet", "fbouquet", Role.STUDENT);
        when(userRepository.existsByLogin("fbouquet")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.createUser(request));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, passwordGenerator);
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
