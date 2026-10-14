package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.dto.ModuleDetails;
import fr.m2.groupe7.kiestla.dto.AdminUserSummary;
import fr.m2.groupe7.kiestla.dto.CreateUserRequest;
import fr.m2.groupe7.kiestla.dto.CreatedUser;
import fr.m2.groupe7.kiestla.dto.UpdateUserRequest;
import fr.m2.groupe7.kiestla.dto.UserSummary;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
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
    private final ModuleRepository moduleRepository;
    private final AbsenceRepository absenceRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            PasswordGenerator passwordGenerator, ModuleRepository moduleRepository,
            AbsenceRepository absenceRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
        this.moduleRepository = moduleRepository;
        this.absenceRepository = absenceRepository;
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

    /**
     * Returns a user, to pre-fill the update form.
     *
     * @param id the user's id
     * @return the user's summary
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    public AdminUserSummary getUser(Long id) {
        return userRepository.findById(id)
                .map(AdminUserSummary::from)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    /**
     * Updates a user's personal information and role. The password is left unchanged.
     * When the role changes, the user's module enrollments are removed since they no longer match.
     *
     * @param id           the user's id
     * @param request      the new personal information and role
     * @param currentLogin the login of the administrator performing the update
     * @return the updated user
     * @throws ResourceNotFoundException if the user does not exist
     * @throws BusinessException         if the login is already used by another user, if the administrator
     *                                   changes their own login or role, or if the role cannot be changed
     */
    @Transactional
    public AdminUserSummary updateUser(Long id, UpdateUserRequest request, String currentLogin) {
        User user = userRepository.findWithModulesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));

        String login = request.login().trim();
        boolean roleChanged = request.role() != user.getRole();

        if (user.getLogin().equals(currentLogin) && (roleChanged || !login.equals(user.getLogin()))) {
            throw new BusinessException("Vous ne pouvez pas modifier votre propre login ni votre propre rôle.");
        }
        if (userRepository.existsByLoginAndIdNot(login, id)) {
            throw new BusinessException("Ce login est déjà utilisé.");
        }
        if (roleChanged) {
            if (moduleRepository.existsByReferenceTeacherId(id)) {
                throw new BusinessException(
                        "Impossible de changer le rôle : cet utilisateur est enseignant référent d'un module.");
            }
            if (absenceRepository.existsByStudentId(id)) {
                throw new BusinessException(
                        "Impossible de changer le rôle : cet utilisateur a des absences enregistrées.");
            }
            user.getModules().clear();
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setLogin(login);
        user.setRole(request.role());

        return AdminUserSummary.from(userRepository.save(user));
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
