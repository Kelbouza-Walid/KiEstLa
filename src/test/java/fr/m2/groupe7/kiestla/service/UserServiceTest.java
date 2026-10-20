package fr.m2.groupe7.kiestla.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import fr.m2.groupe7.kiestla.dto.ModuleDetails;
import fr.m2.groupe7.kiestla.dto.AdminUserDetails;
import fr.m2.groupe7.kiestla.dto.AdminUserSummary;
import fr.m2.groupe7.kiestla.dto.CreateUserRequest;
import fr.m2.groupe7.kiestla.dto.CreatedUser;
import fr.m2.groupe7.kiestla.dto.ModuleSummary;
import fr.m2.groupe7.kiestla.dto.UpdateUserRequest;
import fr.m2.groupe7.kiestla.dto.UserSummary;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.CommentRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.NotifRepository;
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

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private AbsenceRepository absenceRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NotifRepository notifRepository;

    @Mock
    private EmailService emailService;

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
        CreateUserRequest request = new CreateUserRequest(" Fabrice ", " Bouquet ", " fbouquet ", " fbouquet@example.com ", Role.TEACHER);
        when(userRepository.existsByLogin("fbouquet")).thenReturn(false);
        when(userRepository.existsByEmail("fbouquet@example.com")).thenReturn(false);
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
        assertEquals("fbouquet@example.com", saved.getEmail());
        assertEquals(Role.TEACHER, saved.getRole());
        assertEquals("{bcrypt}hashed", saved.getPassword());

        assertEquals(new AdminUserSummary(42L, "Fabrice", "Bouquet", "fbouquet", "fbouquet@example.com", Role.TEACHER), result.user());
        assertEquals("Abc23xyz45Kq", result.generatedPassword());
        verify(emailService).sendAccountCreationEmail(saved, "Abc23xyz45Kq");
    }

    @Test
    void createUser_duplicateLogin() {
        CreateUserRequest request = new CreateUserRequest("Fabrice", "Bouquet", "fbouquet", "fbouquet@example.com", Role.STUDENT);
        when(userRepository.existsByLogin("fbouquet")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.createUser(request));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, passwordGenerator, emailService);
    }

    @Test
    void createUser_duplicateEmail() {
        CreateUserRequest request = new CreateUserRequest("Fabrice", "Bouquet", "fbouquet", "fbouquet@example.com", Role.STUDENT);
        when(userRepository.existsByLogin("fbouquet")).thenReturn(false);
        when(userRepository.existsByEmail("fbouquet@example.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.createUser(request));
        assertEquals("This email is already used.", ex.getMessage());

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, passwordGenerator, emailService);
    }

    @Test
    void createUser_withoutEmail_doesNotSendEmail() {
        CreateUserRequest request = new CreateUserRequest("Fabrice", "Bouquet", "fbouquet", null, Role.TEACHER);
        when(userRepository.existsByLogin("fbouquet")).thenReturn(false);
        when(passwordGenerator.generate()).thenReturn("Abc23xyz45Kq");
        when(passwordEncoder.encode("Abc23xyz45Kq")).thenReturn("{bcrypt}hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createUser(request);

        verifyNoInteractions(emailService);
    }

    @Test
    void getUser_success() {
        when(userRepository.findById(5L))
                .thenReturn(Optional.of(createUser(5L, "Noam", "Faivre", "nfaivre", Role.STUDENT)));

        assertEquals(new AdminUserSummary(5L, "Noam", "Faivre", "nfaivre", Role.STUDENT), userService.getUser(5L));
    }

    @Test
    void getUser_notFound() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUser(5L));
    }

    @Test
    void updateUser_success() {
        User user = createUser(5L, "Noam", "Faivre", "nfaivre", Role.STUDENT);
        user.setPassword("{bcrypt}hashed");
        when(userRepository.findWithModulesById(5L)).thenReturn(Optional.of(user));
        when(userRepository.existsByLoginAndIdNot("nfaivre2", 5L)).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        AdminUserSummary result = userService.updateUser(5L,
                new UpdateUserRequest(" Noa ", " Faivres ", " nfaivre2 ", Role.STUDENT), "admin");

        assertEquals(new AdminUserSummary(5L, "Noa", "Faivres", "nfaivre2", Role.STUDENT), result);
        assertEquals("{bcrypt}hashed", user.getPassword());
        verifyNoInteractions(passwordEncoder, passwordGenerator, moduleRepository, absenceRepository);
    }

    @Test
    void updateUser_keepsOwnLogin() {
        User user = createUser(5L, "Noam", "Faivre", "nfaivre", Role.STUDENT);
        when(userRepository.findWithModulesById(5L)).thenReturn(Optional.of(user));
        when(userRepository.existsByLoginAndIdNot("nfaivre", 5L)).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        AdminUserSummary result = userService.updateUser(5L,
                new UpdateUserRequest("Noam", "Faivre-Martin", "nfaivre", Role.STUDENT), "admin");

        assertEquals("Faivre-Martin", result.lastName());
        assertEquals("nfaivre", result.login());
    }

    @Test
    void updateUser_notFound() {
        when(userRepository.findWithModulesById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(5L,
                new UpdateUserRequest("Noam", "Faivre", "nfaivre", Role.STUDENT), "admin"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_duplicateLogin() {
        User user = createUser(5L, "Noam", "Faivre", "nfaivre", Role.STUDENT);
        when(userRepository.findWithModulesById(5L)).thenReturn(Optional.of(user));
        when(userRepository.existsByLoginAndIdNot("skacmaz", 5L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.updateUser(5L,
                new UpdateUserRequest("Noam", "Faivre", "skacmaz", Role.STUDENT), "admin"));

        assertEquals("nfaivre", user.getLogin());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_adminCannotChangeOwnRole() {
        User admin = createUser(1L, "System", "Admin", "admin", Role.ADMIN);
        when(userRepository.findWithModulesById(1L)).thenReturn(Optional.of(admin));

        assertThrows(BusinessException.class, () -> userService.updateUser(1L,
                new UpdateUserRequest("System", "Admin", "admin", Role.STUDENT), "admin"));

        assertEquals(Role.ADMIN, admin.getRole());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_adminCannotChangeOwnLogin() {
        User admin = createUser(1L, "System", "Admin", "admin", Role.ADMIN);
        when(userRepository.findWithModulesById(1L)).thenReturn(Optional.of(admin));

        assertThrows(BusinessException.class, () -> userService.updateUser(1L,
                new UpdateUserRequest("System", "Admin", "root", Role.ADMIN), "admin"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_adminCanChangeOwnName() {
        User admin = createUser(1L, "System", "Admin", "admin", Role.ADMIN);
        when(userRepository.findWithModulesById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.existsByLoginAndIdNot("admin", 1L)).thenReturn(false);
        when(userRepository.save(admin)).thenReturn(admin);

        AdminUserSummary result = userService.updateUser(1L,
                new UpdateUserRequest("Super", "Admin", "admin", Role.ADMIN), "admin");

        assertEquals("Super", result.firstName());
    }

    @Test
    void updateUser_roleChangeRemovesEnrollments() {
        User user = createUser(5L, "Noam", "Faivre", "nfaivre", Role.TEACHER);
        user.getModules().add(new Module());
        when(userRepository.findWithModulesById(5L)).thenReturn(Optional.of(user));
        when(userRepository.existsByLoginAndIdNot("nfaivre", 5L)).thenReturn(false);
        when(moduleRepository.existsByReferenceTeacherId(5L)).thenReturn(false);
        when(absenceRepository.existsByStudentId(5L)).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        AdminUserSummary result = userService.updateUser(5L,
                new UpdateUserRequest("Noam", "Faivre", "nfaivre", Role.SECRETARY), "admin");

        assertEquals(Role.SECRETARY, result.role());
        assertTrue(user.getModules().isEmpty());
    }

    @Test
    void updateUser_roleChangeRefusedForReferenceTeacher() {
        User teacher = createUser(5L, "Fabrice", "Bouquet", "fbouquet", Role.TEACHER);
        when(userRepository.findWithModulesById(5L)).thenReturn(Optional.of(teacher));
        when(userRepository.existsByLoginAndIdNot("fbouquet", 5L)).thenReturn(false);
        when(moduleRepository.existsByReferenceTeacherId(5L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.updateUser(5L,
                new UpdateUserRequest("Fabrice", "Bouquet", "fbouquet", Role.STUDENT), "admin"));

        assertEquals(Role.TEACHER, teacher.getRole());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_roleChangeRefusedForStudentWithAbsences() {
        User student = createUser(5L, "Noam", "Faivre", "nfaivre", Role.STUDENT);
        when(userRepository.findWithModulesById(5L)).thenReturn(Optional.of(student));
        when(userRepository.existsByLoginAndIdNot("nfaivre", 5L)).thenReturn(false);
        when(moduleRepository.existsByReferenceTeacherId(5L)).thenReturn(false);
        when(absenceRepository.existsByStudentId(5L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.updateUser(5L,
                new UpdateUserRequest("Noam", "Faivre", "nfaivre", Role.TEACHER), "admin"));

        assertEquals(Role.STUDENT, student.getRole());
        verify(userRepository, never()).save(any());
    }

    @Test
    void searchUsers_noCriteriaReturnsAllUsers() {
        List<User> users = searchableUsers();
        when(userRepository.findAll()).thenReturn(users);

        List<AdminUserSummary> result = userService.searchUsers("  ", null);

        assertEquals(users.stream().map(AdminUserSummary::from).toList(), result);
    }

    @Test
    void searchUsers_byNameIgnoresCase() {
        when(userRepository.findAll()).thenReturn(searchableUsers());

        List<AdminUserSummary> result = userService.searchUsers(" FAIV ", null);

        assertEquals(List.of("nfaivre"), logins(result));
    }

    @Test
    void searchUsers_byLogin() {
        when(userRepository.findAll()).thenReturn(searchableUsers());

        assertEquals(List.of("skacmaz"), logins(userService.searchUsers("skac", null)));
    }

    @Test
    void searchUsers_byRoleOnlySortedByName() {
        when(userRepository.findAll()).thenReturn(searchableUsers());

        List<AdminUserSummary> result = userService.searchUsers(null, Role.STUDENT);

        // sorted by last name: Faivre, Kacmaz
        assertEquals(List.of("nfaivre", "skacmaz"), logins(result));
    }

    @Test
    void searchUsers_byNameAndRole() {
        when(userRepository.findAll()).thenReturn(searchableUsers());

        // "f" matches Noam Faivre and Fabrice Bouquet, the role keeps only the student
        assertEquals(List.of("nfaivre"), logins(userService.searchUsers("f", Role.STUDENT)));
    }

    @Test
    void searchUsers_noMatch() {
        when(userRepository.findAll()).thenReturn(searchableUsers());

        assertTrue(userService.searchUsers("zzz", null).isEmpty());
    }

    @Test
    void searchUsers_byEmail() {
        User userWithEmail = createUser(2L, "Suleyman", "Kacmaz", "skacmaz", Role.STUDENT);
        userWithEmail.setEmail("skacmaz@univ-fcomte.fr");
        when(userRepository.findAll()).thenReturn(List.of(
                createUser(1L, "System", "Admin", "admin", Role.ADMIN),
                userWithEmail
        ));

        List<AdminUserSummary> result = userService.searchUsers("fcomte", null);

        assertEquals(List.of("skacmaz"), logins(result));
    }

    @Test
    void findByEmail_found() {
        User user = createUser(2L, "Suleyman", "Kacmaz", "skacmaz", Role.STUDENT);
        user.setEmail("skacmaz@example.com");
        when(userRepository.findByEmail("skacmaz@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail(" SKACMAZ@EXAMPLE.COM ");

        assertTrue(result.isPresent());
        assertEquals("skacmaz", result.get().getLogin());
    }

    @Test
    void findByEmail_blankOrNotFound() {
        assertTrue(userService.findByEmail(" ").isEmpty());
        assertTrue(userService.findByEmail(null).isEmpty());

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        assertTrue(userService.findByEmail("unknown@example.com").isEmpty());
    }

    private List<User> searchableUsers() {
        return List.of(
                createUser(1L, "System", "Admin", "admin", Role.ADMIN),
                createUser(2L, "Suleyman", "Kacmaz", "skacmaz", Role.STUDENT),
                createUser(3L, "Noam", "Faivre", "nfaivre", Role.STUDENT),
                createUser(4L, "Fabrice", "Bouquet", "fbouquet", Role.TEACHER));
    }

    private List<String> logins(List<AdminUserSummary> users) {
        return users.stream().map(AdminUserSummary::login).toList();
    }

    @Test
    void getUserDetails_student() {
        User student = createUser(5L, "Noam", "Faivre", "nfaivre", Role.STUDENT);
        Module physics = createModule(2L, "Physique");
        Module algorithms = createModule(1L, "Algorithmique");
        student.getModules().add(physics);
        student.getModules().add(algorithms);
        when(userRepository.findWithModulesById(5L)).thenReturn(Optional.of(student));
        when(moduleRepository.findByReferenceTeacherIdOrderByName(5L)).thenReturn(List.of());
        when(absenceRepository.countByStudentId(5L)).thenReturn(3L);

        AdminUserDetails result = userService.getUserDetails(5L);

        assertEquals(new AdminUserDetails(5L, "Noam", "Faivre", "nfaivre", Role.STUDENT,
                List.of(ModuleSummary.from(algorithms), ModuleSummary.from(physics)), List.of(), 3L), result);
    }

    @Test
    void getUserDetails_referenceTeacher() {
        User teacher = createUser(10L, "Fabrice", "Bouquet", "fbouquet", Role.TEACHER);
        Module module = createModule(1L, "Test fonctionnel");
        module.setReferenceTeacher(teacher);
        when(userRepository.findWithModulesById(10L)).thenReturn(Optional.of(teacher));
        when(moduleRepository.findByReferenceTeacherIdOrderByName(10L)).thenReturn(List.of(module));
        when(absenceRepository.countByStudentId(10L)).thenReturn(0L);

        AdminUserDetails result = userService.getUserDetails(10L);

        assertEquals(List.of(new ModuleSummary(1L, "Test fonctionnel", "Bouquet")), result.referencedModules());
        assertTrue(result.enrolledModules().isEmpty());
    }

    @Test
    void getUserDetails_notFound() {
        when(userRepository.findWithModulesById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserDetails(5L));
    }

    @Test
    void deleteUser_success() {
        User user = createUser(10L, "John", "Doe", "jdoe", Role.STUDENT);
        Module module = createModule(1L, "Mathématiques");
        user.getModules().add(module);

        when(userRepository.findWithModulesById(10L)).thenReturn(Optional.of(user));
        when(moduleRepository.findByReferenceTeacherIdOrderByName(10L)).thenReturn(List.of());
        when(commentRepository.findByCommenterId(10L)).thenReturn(List.of());
        when(absenceRepository.findByStudentId(10L)).thenReturn(List.of());
        when(notifRepository.findByUserId(10L)).thenReturn(List.of());

        AdminUserSummary deleted = userService.deleteUser(10L, "admin");

        assertEquals(10L, deleted.id());
        assertEquals("John", deleted.firstName());
        assertEquals("Doe", deleted.lastName());
        assertEquals("jdoe", deleted.login());
        assertTrue(user.getModules().isEmpty());
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_cannotDeleteOwnAccount() {
        User admin = createUser(1L, "Admin", "User", "admin", Role.ADMIN);
        when(userRepository.findWithModulesById(1L)).thenReturn(Optional.of(admin));

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.deleteUser(1L, "admin"));
        assertEquals("You cannot delete your own account.", ex.getMessage());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUser_notFound() {
        when(userRepository.findWithModulesById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(99L, "admin"));
        verify(userRepository, never()).delete(any());
    }

    private Module createModule(Long id, String name) {
        Module module = new Module();
        module.setId(id);
        module.setName(name);
        return module;
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
