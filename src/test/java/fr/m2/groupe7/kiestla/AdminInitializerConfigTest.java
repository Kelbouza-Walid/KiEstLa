package fr.m2.groupe7.kiestla;

import fr.m2.groupe7.kiestla.config.AdminInitializerConfig;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInitializerConfigTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final AdminInitializerConfig config = new AdminInitializerConfig();

    @Test
    void shouldCreateAdminWhenCredentialsProvidedAndUserDoesNotExist() throws Exception {
        when(userRepository.existsByLogin("superadmin")).thenReturn(false);
        when(passwordEncoder.encode("secretPass123")).thenReturn("hashedPassword");

        CommandLineRunner runner = config.initAdminUser(userRepository, passwordEncoder, "superadmin", "secretPass123");
        runner.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User created = captor.getValue();
        assertEquals("superadmin", created.getLogin());
        assertEquals("hashedPassword", created.getPassword());
        assertEquals(Role.ADMIN, created.getRole());
        assertEquals("System", created.getFirstName());
        assertEquals("Admin", created.getLastName());
    }

    @Test
    void shouldNotCreateAdminWhenUserAlreadyExists() throws Exception {
        when(userRepository.existsByLogin("superadmin")).thenReturn(true);

        CommandLineRunner runner = config.initAdminUser(userRepository, passwordEncoder, "superadmin", "secretPass123");
        runner.run();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNotCreateAdminWhenUsernameOrPasswordIsBlank() throws Exception {
        CommandLineRunner runner1 = config.initAdminUser(userRepository, passwordEncoder, "", "secret");
        runner1.run();

        CommandLineRunner runner2 = config.initAdminUser(userRepository, passwordEncoder, "admin", " ");
        runner2.run();

        CommandLineRunner runner3 = config.initAdminUser(userRepository, passwordEncoder, null, null);
        runner3.run();

        verify(userRepository, never()).existsByLogin(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
