package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.entity.PasswordResetToken;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.PasswordResetTokenRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Password-related use cases, including authenticated password change
 * and token-based password reset via email.
 */
@Service
public class PasswordService {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int TOKEN_EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    @Autowired
    public PasswordService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    public PasswordService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, null, null);
    }

    /**
     * Changes the password of the user identified by login.
     * The current password must be given and correct.
     *
     * @throws ResourceNotFoundException if the user doesn't exist
     * @throws BusinessException         if the current password is wrong, the new one is too
     *                                   short, the confirmation does not match, or the new
     *                                   password is the same as the current one
     */
    @Transactional
    public void changePassword(String login, String currentPassword, String newPassword,
                               String confirmPassword) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("utilisateur non trouvé : " + login));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException("Le mot de passe entré est incorrect");
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(
                    "Le nouveau mot de passe doit contenir au moins " + MIN_PASSWORD_LENGTH + " caractères");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("La confirmation ne correspond pas au nouveau mot de passe");
        }
        // currentPassword was just verified, so comparing the two plain texts is enough
        if (newPassword.equals(currentPassword)) {
            throw new BusinessException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Requests a password reset for a user identified by login or email.
     * Generates a reset token and sends an email with the reset link.
     *
     * @param loginOrEmail login or email of the user
     * @param baseUrl base URL of the application (e.g. http://localhost:8080)
     * @return the created reset token
     */
    @Transactional
    public PasswordResetToken requestPasswordReset(String loginOrEmail, String baseUrl) {
        if (loginOrEmail == null || loginOrEmail.isBlank()) {
            throw new BusinessException("Please provide a username or email address.");
        }

        Optional<User> userOpt = userRepository.findByLogin(loginOrEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(loginOrEmail);
        }

        User user = userOpt.orElseThrow(() ->
                new BusinessException("No account found with this identifier."));

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException("This account does not have an email address configured.");
        }

        String tokenValue = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken(
                tokenValue,
                user,
                LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES)
        );

        if (tokenRepository != null) {
            token = tokenRepository.save(token);
        }

        if (emailService != null) {
            String sanitizedBase = (baseUrl != null) ? baseUrl.replaceAll("/+$", "") : "";
            String resetLink = sanitizedBase + "/reset-password?token=" + tokenValue;
            emailService.sendPasswordResetEmail(user, resetLink);
        }

        return token;
    }

    /**
     * Validates a password reset token.
     *
     * @param tokenValue the token string from the reset link
     * @return the valid PasswordResetToken
     * @throws BusinessException if the token is missing, not found, expired, or already used
     */
    @Transactional(readOnly = true)
    public PasswordResetToken validateResetToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new BusinessException("Invalid password reset link.");
        }

        if (tokenRepository == null) {
            throw new BusinessException("Token repository not configured.");
        }

        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BusinessException("Invalid or expired password reset link."));

        if (token.isUsed()) {
            throw new BusinessException("This password reset link has already been used.");
        }
        if (token.isExpired()) {
            throw new BusinessException("This password reset link has expired.");
        }

        return token;
    }

    /**
     * Resets the user's password using a valid reset token.
     *
     * @param tokenValue the reset token
     * @param newPassword the new password
     * @param confirmPassword confirmation of the new password
     */
    @Transactional
    public void resetPassword(String tokenValue, String newPassword, String confirmPassword) {
        PasswordResetToken token = validateResetToken(tokenValue);

        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(
                    "The new password must contain at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("The confirmation does not match the new password.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        if (tokenRepository != null) {
            tokenRepository.save(token);
        }
    }
}