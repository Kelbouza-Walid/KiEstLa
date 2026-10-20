package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.entity.PasswordResetToken;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.PasswordResetTokenRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

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

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

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

    // --- Password reset tests ---

    @Test
    void requestPasswordReset_success_withLogin() {
        User user = createUser("teacher1", "hashedPass");
        user.setEmail("teacher1@univ.fr");

        when(userRepository.findByLogin("teacher1")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));

        PasswordResetToken token = passwordService.requestPasswordReset("teacher1", "http://localhost:8080");

        assertNotNull(token);
        assertEquals(user, token.getUser());
        assertNotNull(token.getToken());
        assertFalse(token.isUsed());
        assertFalse(token.isExpired());

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(user), linkCaptor.capture());
        assertTrue(linkCaptor.getValue().startsWith("http://localhost:8080/reset-password?token="));
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void requestPasswordReset_success_withEmail() {
        User user = createUser("student1", "hashedPass");
        user.setEmail("student1@univ.fr");

        when(userRepository.findByLogin("student1@univ.fr")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("student1@univ.fr")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));

        PasswordResetToken token = passwordService.requestPasswordReset("student1@univ.fr", "http://localhost:8080/");

        assertNotNull(token);
        assertEquals(user, token.getUser());
        verify(emailService).sendPasswordResetEmail(eq(user), startsWith("http://localhost:8080/reset-password?token="));
    }

    @Test
    void requestPasswordReset_nullOrBlank_throwsException() {
        assertThrows(BusinessException.class,
                () -> passwordService.requestPasswordReset("   ", "http://localhost:8080"));
        verifyNoInteractions(userRepository, tokenRepository, emailService);
    }

    @Test
    void requestPasswordReset_userNotFound_throwsException() {
        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> passwordService.requestPasswordReset("unknown", "http://localhost:8080"));
        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void requestPasswordReset_userHasNoEmail_throwsException() {
        User userWithoutEmail = createUser("noemail", "hashed");
        userWithoutEmail.setEmail(null);

        when(userRepository.findByLogin("noemail")).thenReturn(Optional.of(userWithoutEmail));

        assertThrows(BusinessException.class,
                () -> passwordService.requestPasswordReset("noemail", "http://localhost:8080"));
        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void validateResetToken_success() {
        User user = createUser("walid1", "hashed");
        PasswordResetToken token = new PasswordResetToken("valid-token", user, LocalDateTime.now().plusMinutes(20));

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        PasswordResetToken result = passwordService.validateResetToken("valid-token");
        assertSame(token, result);
    }

    @Test
    void validateResetToken_nullOrBlank_throwsException() {
        assertThrows(BusinessException.class, () -> passwordService.validateResetToken("  "));
    }

    @Test
    void validateResetToken_notFound_throwsException() {
        when(tokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> passwordService.validateResetToken("missing"));
    }

    @Test
    void validateResetToken_alreadyUsed_throwsException() {
        User user = createUser("walid1", "hashed");
        PasswordResetToken token = new PasswordResetToken("used-token", user, LocalDateTime.now().plusMinutes(20));
        token.setUsed(true);

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThrows(BusinessException.class, () -> passwordService.validateResetToken("used-token"));
    }

    @Test
    void validateResetToken_expired_throwsException() {
        User user = createUser("walid1", "hashed");
        PasswordResetToken token = new PasswordResetToken("expired-token", user, LocalDateTime.now().minusMinutes(5));

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThrows(BusinessException.class, () -> passwordService.validateResetToken("expired-token"));
    }

    @Test
    void resetPassword_success() {
        User user = createUser("walid1", "oldHash");
        PasswordResetToken token = new PasswordResetToken("token123", user, LocalDateTime.now().plusMinutes(15));

        when(tokenRepository.findByToken("token123")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewSecret123")).thenReturn("newHash");

        passwordService.resetPassword("token123", "NewSecret123", "NewSecret123");

        assertEquals("newHash", user.getPassword());
        assertTrue(token.isUsed());
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void resetPassword_tooShort_throwsException() {
        User user = createUser("walid1", "oldHash");
        PasswordResetToken token = new PasswordResetToken("token123", user, LocalDateTime.now().plusMinutes(15));

        when(tokenRepository.findByToken("token123")).thenReturn(Optional.of(token));

        assertThrows(BusinessException.class,
                () -> passwordService.resetPassword("token123", "short", "short"));

        assertFalse(token.isUsed());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_confirmationMismatch_throwsException() {
        User user = createUser("walid1", "oldHash");
        PasswordResetToken token = new PasswordResetToken("token123", user, LocalDateTime.now().plusMinutes(15));

        when(tokenRepository.findByToken("token123")).thenReturn(Optional.of(token));

        assertThrows(BusinessException.class,
                () -> passwordService.resetPassword("token123", "Password123", "Different123"));

        assertFalse(token.isUsed());
        verify(userRepository, never()).save(any());
    }

    private User createUser(String login, String password) {
        User user = new User();
        user.setLogin(login);
        user.setPassword(password);
        return user;
    }
}