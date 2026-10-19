package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service managing email delivery for Kiestla.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:pam.tp.spring@gmail.com}")
    private String fromEmail = "pam.tp.spring@gmail.com";

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an account creation email containing initial credentials to a user.
     *
     * @param user the newly created user
     * @param clearPassword the raw generated password
     */
    public void sendAccountCreationEmail(User user, String clearPassword) {
        if (user == null) {
            return;
        }
        sendAccountCreationEmail(user.getEmail(), user.getFirstName(), user.getLogin(), clearPassword);
    }

    /**
     * Sends an account creation email containing initial credentials.
     *
     * @param to the recipient email address
     * @param firstName the recipient's first name
     * @param login the recipient's login
     * @param clearPassword the raw generated password
     */
    public void sendAccountCreationEmail(String to, String firstName, String login, String clearPassword) {
        if (to == null || to.isBlank()) {
            return;
        }
        String subject = "Welcome to kiestla - Your account details";
        String body = buildAccountCreationBody(firstName, login, clearPassword);
        sendEmail(to, subject, body);
    }

    /**
     * Builds the body of the account creation email in English.
     */
    public String buildAccountCreationBody(String firstName, String login, String password) {
        String name = (firstName != null && !firstName.isBlank()) ? firstName.trim() : login;
        return "Hello " + name + ",\n\n" +
                "Your profile has just been added to kiestla.\n" +
                "In order to log in, here is your information:\n" +
                "login : " + login + "\n" +
                "password : " + password + "\n\n" +
                "Best regards.";
    }

    /**
     * Sends a password reset email to a user with the reset link.
     *
     * @param user the user requesting password reset
     * @param resetLink the full URL to the password reset page
     */
    public void sendPasswordResetEmail(User user, String resetLink) {
        if (user == null) {
            return;
        }
        sendPasswordResetEmail(user.getEmail(), user.getFirstName(), user.getLogin(), resetLink);
    }

    /**
     * Sends a password reset email with the reset link.
     *
     * @param to recipient email
     * @param firstName recipient first name
     * @param login recipient login
     * @param resetLink full URL to reset page
     */
    public void sendPasswordResetEmail(String to, String firstName, String login, String resetLink) {
        if (to == null || to.isBlank()) {
            return;
        }
        String subject = "Reset your Kiestla password";
        String body = buildPasswordResetBody(firstName, login, resetLink);
        sendEmail(to, subject, body);
    }

    /**
     * Builds the body of the password reset email in English.
     */
    public String buildPasswordResetBody(String firstName, String login, String resetLink) {
        String name = (firstName != null && !firstName.isBlank()) ? firstName.trim() : login;
        return "Hello " + name + ",\n\n" +
                "You requested to reset your password for your Kiestla account.\n" +
                "Please click the link below to set a new password:\n" +
                resetLink + "\n\n" +
                "This link will expire in 30 minutes.\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "The Kiestla Team";
    }

    /**
     * Sends a simple text email.
     *
     * @param to recipient email
     * @param subject email subject
     * @param body email body text
     */
    public void sendEmail(String to, String subject, String body) {
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Email to '{}' was not sent.", to);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (fromEmail != null && !fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (MailException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }
}
