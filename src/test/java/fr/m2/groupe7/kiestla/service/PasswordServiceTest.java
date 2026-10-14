package fr.m2.groupe7.kiestla.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PasswordService.
 */
@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordService passwordService;

    @Test
    void changePassword_basic() {
        User user = createUser("walid1", "hashedOld");
        when(userRepository.findByLogin("walid1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "hashedOld")).thenReturn(true);
        when(passwordEncoder.encode("newPassword1")).thenReturn("hashedNew");

        passwordService.changePassword("walid1", "oldPassword", "newPassword1", "newPassword1");

        assertEquals("hashedNew", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_userNotFound() {
        when(userRepository.findByLogin("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> passwordService.changePassword("ghost", "oldPassword", "newPassword1", "newPassword1"));
    }

    @Test
    void changePassword_wrongCurrentPassword() {
        User user = createUser("walid1", "hashedOld");
        when(userRepository.findByLogin("walid1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedOld")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> passwordService.changePassword("walid1", "wrongPassword", "newPassword1", "newPassword1"));

        assertEquals("hashedOld", user.getPassword());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_newPasswordTooShort() {
        User user = createUser("walid1", "hashedOld");
        when(userRepository.findByLogin("walid1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "hashedOld")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> passwordService.changePassword("walid1", "oldPassword", "short1", "short1"));

        assertEquals("hashedOld", user.getPassword());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_confirmationDoesNotMatch() {
        User user = createUser("walid1", "hashedOld");
        when(userRepository.findByLogin("walid1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "hashedOld")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> passwordService.changePassword("walid1", "oldPassword", "newPassword1", "somethingElse"));

        assertEquals("hashedOld", user.getPassword());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_sameAsCurrentPassword() {
        User user = createUser("walid1", "hashedOld");
        when(userRepository.findByLogin("walid1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "hashedOld")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> passwordService.changePassword("walid1", "oldPassword", "oldPassword", "oldPassword"));

        assertEquals("hashedOld", user.getPassword());
        verify(userRepository, never()).save(any());
    }

    private User createUser(String login, String password) {
        User user = new User();
        user.setLogin(login);
        user.setPassword(password);
        return user;
    }
}