package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.dto.ModuleDetails;
import fr.m2.groupe7.kiestla.dto.AdminUserDetails;
import fr.m2.groupe7.kiestla.dto.AdminUserSummary;
import fr.m2.groupe7.kiestla.dto.CreateUserRequest;
import fr.m2.groupe7.kiestla.dto.CreatedUser;
import fr.m2.groupe7.kiestla.dto.ModuleSummary;
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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
     * Searches users by name and/or role.
     *
     * @param query text to look for in the first name, last name or login (case insensitive), may be blank
     * @param role  role to filter on, or {@code null} for all roles
     * @return the matching users, or all users when no criteria is given
     */
    @Transactional(readOnly = true)
    public List<AdminUserSummary> searchUsers(String query, Role role) {
        boolean noQuery = query == null || query.isBlank();
        if (noQuery && role == null) {
            return getAllUsers();
        }
        String pattern = "%" + (noQuery ? "" : query.trim().toLowerCase(Locale.ROOT)) + "%";
        return userRepository.search(pattern, role).stream()
                .map(AdminUserSummary::from)
                .toList();
    }

    /**
     * Returns everything an administrator can see about a user.
     *
     * @param id the user's id
     * @return the user's information, modules and number of absences
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    public AdminUserDetails getUserDetails(Long id) {
        User user = userRepository.findWithModulesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        List<ModuleSummary> enrolledModules = user.getModules().stream()
                .map(ModuleSummary::from)
                .sorted(Comparator.comparing(ModuleSummary::name))
                .toList();
        List<ModuleSummary> referencedModules = moduleRepository.findByReferenceTeacherIdOrderByName(id).stream()
                .map(ModuleSummary::from)
                .toList();

        return new AdminUserDetails(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getLogin(),
                user.getRole(),
                enrolledModules,
                referencedModules,
                absenceRepository.countByStudentId(id));
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
            throw new BusinessException("This login is already used.");
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        String login = request.login().trim();
        boolean roleChanged = request.role() != user.getRole();

        if (user.getLogin().equals(currentLogin) && (roleChanged || !login.equals(user.getLogin()))) {
            throw new BusinessException("You cannot change your own login or role.");
        }
        if (userRepository.existsByLoginAndIdNot(login, id)) {
            throw new BusinessException("This login is already used.");
        }
        if (roleChanged) {
            if (moduleRepository.existsByReferenceTeacherId(id)) {
                throw new BusinessException(
                        "Cannot change the role: this user is the reference teacher of a module.");
            }
            if (absenceRepository.existsByStudentId(id)) {
                throw new BusinessException(
                        "Cannot change the role: this user has recorded absences.");
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
