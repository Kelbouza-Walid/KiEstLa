package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.dto.ModuleDetails;
import fr.m2.groupe7.kiestla.dto.AdminUserSummary;
import fr.m2.groupe7.kiestla.dto.CreateUserRequest;
import fr.m2.groupe7.kiestla.dto.CreatedUser;
import fr.m2.groupe7.kiestla.dto.UserSummary;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            PasswordGenerator passwordGenerator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
    }

    @Transactional(readOnly = true)
    public List<AdminUserSummary> getAllUsers() {
        return userRepository.findAll().stream()
                .map(AdminUserSummary::from)
                .toList();
    }

    /**
     * Creates a new user with a randomly generated password.
     *
     * @param request the user's personal information and role
     * @return the created user and its generated password (in clear, to be shown once)
     * @throws BusinessException if the login is already used
     */
    @Transactional
    public CreatedUser createUser(CreateUserRequest request) {
        String login = request.login().trim();
        if (userRepository.existsByLogin(login)) {
            throw new BusinessException("Ce login est déjà utilisé.");
        }

        String generatedPassword = passwordGenerator.generate();

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setLogin(login);
        user.setPassword(passwordEncoder.encode(generatedPassword));
        user.setRole(request.role());

        User saved = userRepository.save(user);
        return new CreatedUser(AdminUserSummary.from(saved), generatedPassword);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> getAllTeachers() {
        return userRepository.findByRole(Role.TEACHER).stream()
                .map(UserSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummary> getAvailableStudentsForModule(ModuleDetails module) {
        Set<Long> enrolledStudentIds = module.students().stream()
                .map(UserSummary::id)
                .collect(Collectors.toSet());

        return userRepository.findByRole(Role.STUDENT).stream()
                .filter(student -> !enrolledStudentIds.contains(student.getId()))
                .map(UserSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummary> getAvailableTeachersForModule(ModuleDetails module) {
        Set<Long> enrolledTeacherIds = module.teachers().stream()
                .map(UserSummary::id)
                .collect(Collectors.toSet());

        return userRepository.findByRole(Role.TEACHER).stream()
                .filter(teacher -> !enrolledTeacherIds.contains(teacher.getId()))
                .map(UserSummary::from)
                .toList();
    }
}
