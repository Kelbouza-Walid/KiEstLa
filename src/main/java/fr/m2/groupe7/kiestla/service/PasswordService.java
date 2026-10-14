package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Password-related use cases
 * Kept separate from UserService (the password recovery ticket will live here too)
 */
@Service
public class PasswordService {

    public static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Changes the password of the user identified by login
     * The current password must be given and correct
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
}