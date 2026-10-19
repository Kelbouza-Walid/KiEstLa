package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendAccountCreationEmail_success() {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setLogin("jdoe");
        user.setEmail("jdoe@example.com");
        user.setRole(Role.STUDENT);

        emailService.sendAccountCreationEmail(user, "SecurePass123!");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("pam.tp.spring@gmail.com", message.getFrom());
        assertArrayEquals(new String[]{"jdoe@example.com"}, message.getTo());
        assertEquals("Welcome to kiestla - Your account details", message.getSubject());

        String body = Objects.requireNonNull(message.getText());
        assertTrue(body.contains("Hello John,"));
        assertTrue(body.contains("Your profile has just been added to kiestla."));
        assertTrue(body.contains("In order to log in, here is your information:"));
        assertTrue(body.contains("login : jdoe"));
        assertTrue(body.contains("password : SecurePass123!"));
        assertTrue(body.contains("Best regards."));
    }

    @Test
    void sendAccountCreationEmail_nullUser_doesNothing() {
        emailService.sendAccountCreationEmail((User) null, "pass");

        verifyNoInteractions(mailSender);
    }

    @Test
    void sendAccountCreationEmail_nullOrBlankEmail_doesNothing() {
        User userWithoutEmail = new User();
        userWithoutEmail.setFirstName("John");
        userWithoutEmail.setLogin("jdoe");
        userWithoutEmail.setEmail("   ");

        emailService.sendAccountCreationEmail(userWithoutEmail, "pass");

        verifyNoInteractions(mailSender);
    }

    @Test
    void sendAccountCreationEmail_blankFirstName_fallsBackToLogin() {
        emailService.sendAccountCreationEmail("user@example.com", "   ", "cooluser", "SecretPass");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        String body = Objects.requireNonNull(captor.getValue().getText());
        assertTrue(body.contains("Hello cooluser,"));
        assertTrue(body.contains("login : cooluser"));
        assertTrue(body.contains("password : SecretPass"));
    }

    @Test
    void sendEmail_whenMailSenderThrowsException_doesNotPropagate() {
        doThrow(new MailSendException("SMTP connection failed")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendAccountCreationEmail("fail@example.com", "Jane", "jane", "pass")
        );
    }

    @Test
    void sendEmail_whenMailSenderIsNull_doesNotThrow() {
        EmailService serviceWithoutSender = new EmailService(null);

        assertDoesNotThrow(() ->
                serviceWithoutSender.sendAccountCreationEmail("any@example.com", "Jane", "jane", "pass")
        );
    }

    @Test
    void sendPasswordResetEmail_success() {
        User user = new User();
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setLogin("alice");
        user.setEmail("alice@example.com");

        emailService.sendPasswordResetEmail(user, "http://localhost:8080/reset-password?token=test-token-123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("pam.tp.spring@gmail.com", message.getFrom());
        assertArrayEquals(new String[]{"alice@example.com"}, message.getTo());
        assertEquals("Reset your Kiestla password", message.getSubject());

        String body = Objects.requireNonNull(message.getText());
        assertTrue(body.contains("Hello Alice,"));
        assertTrue(body.contains("You requested to reset your password for your Kiestla account."));
        assertTrue(body.contains("http://localhost:8080/reset-password?token=test-token-123"));
        assertTrue(body.contains("This link will expire in 30 minutes."));
        assertTrue(body.contains("The Kiestla Team"));
    }

    @Test
    void sendPasswordResetEmail_nullUserOrEmail_doesNothing() {
        emailService.sendPasswordResetEmail((User) null, "http://link");

        User userWithoutEmail = new User();
        userWithoutEmail.setEmail("");
        emailService.sendPasswordResetEmail(userWithoutEmail, "http://link");

        verifyNoInteractions(mailSender);
    }
}
