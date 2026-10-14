package fr.m2.groupe7.kiestla.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import fr.m2.groupe7.kiestla.dto.ModuleSummary;
import fr.m2.groupe7.kiestla.dto.ModuleDetails;
import fr.m2.groupe7.kiestla.dto.ModuleMembers;
import fr.m2.groupe7.kiestla.dto.UserSummary;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;

@Service
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final AbsenceRepository absenceRepository;

    private static final Comparator<User> BY_NAME = Comparator.comparing(User::getLastName)
            .thenComparing(User::getFirstName);

    public ModuleService(ModuleRepository moduleRepository, UserRepository userRepository,
            AbsenceRepository absenceRepository) {
        this.moduleRepository = moduleRepository;
        this.userRepository = userRepository;
        this.absenceRepository = absenceRepository;
    }

    /**
     * Returns all a ModuleSummary for every module.
     */
    @Transactional(readOnly = true)
    public List<ModuleSummary> getAllModulesSummary() {
        return moduleRepository.findAll()
                .stream()
                .map(ModuleSummary::from)
                .toList();
    }

    /**
     * Returns the Module details of a module given it's id.
     *
     * @trows ResourceNotFoundException if the module does not exist
     */
    @Transactional(readOnly = true)
    public ModuleDetails getModuleDetails(Long id) {
        return moduleRepository.findWithStudentsById(id)
                .map(ModuleDetails::from)
                .orElseThrow(() -> new ResourceNotFoundException("Module introuvable : " + id));
    }

    /**
     * Same as getModuleDetails, but first checks that teacherLogin may access the
     * module (assigned or reference teacher).
     *
     * @throws ResourceNotFoundException if the user or module does not exist
     * @throws BusinessException         if the teacher has no access to the module
     */
    @Transactional(readOnly = true)
    public ModuleDetails getModuleDetailsForTeacher(Long moduleId, String teacherLogin) {
        return ModuleDetails.from(verifyTeacherAccess(moduleId, teacherLogin));
    }

    @Transactional(readOnly = true)
    public boolean isReferenceTeacher(Long moduleId, String login) {
        return moduleRepository.findById(moduleId)
                .map(module -> isReferenceTeacher(module, login))
                .orElse(false);
    }

    private boolean isReferenceTeacher(Module module, String login) {
        return module.getReferenceTeacher() != null
                && module.getReferenceTeacher().getLogin().equals(login);
    }

    /**
     * Returns a ModuleSummary for every modules of a teacher.
     *
     * @throws ResourceNotFoundException if the user or module does not exist
     */
    @Transactional(readOnly = true)
    public List<ModuleSummary> getModulesForTeacher(String teacherLogin) {
        User teacher = userRepository.findModulesByLogin(teacherLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé : " + teacherLogin));
        return teacher.getModules().stream()
                .map(ModuleSummary::from)
                .toList();
    }

    /**
     * Verifies that teacherLogin has access to the given module (assigned or
     * reference teacher).
     * Returns the Module (with students and referenceTeacher) so callers can
     * reuse it without an extra DB query.
     *
     * @throws ResourceNotFoundException if the user or module does not exist
     * @throws BusinessException         if the teacher has no access to the module
     */
    private Module verifyTeacherAccess(Long moduleId, String teacherLogin) {
        User teacher = userRepository.findModulesByLogin(teacherLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé : " + teacherLogin));

        Module module = moduleRepository.findWithStudentsById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module introuvable : " + moduleId));

        boolean assignedToTeacher = teacher.getModules().stream()
                .anyMatch(m -> m.getId().equals(moduleId));
        boolean isReferenceTeacher = isReferenceTeacher(module, teacherLogin);

        if (!assignedToTeacher && !isReferenceTeacher) {
            throw new BusinessException("Vous n'êtes pas autorisé à accéder à ce module.");
        }

        return module;
    }

    @Transactional
    public void enrollStudent(Long moduleId, Long studentId, String teacherLogin) {
        // verifyTeacherAccess already loads the module — reuse it to avoid a second DB
        // hit
        Module module = verifyTeacherAccess(moduleId, teacherLogin);

        User student = userRepository.findWithModulesById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant introuvable : " + studentId));

        if (student.getRole() != Role.STUDENT) {
            throw new BusinessException("L'utilisateur sélectionné n'est pas un étudiant.");
        }

        boolean alreadyEnrolled = student.getModules().stream()
                .anyMatch(m -> m.getId().equals(moduleId));
        if (alreadyEnrolled) {
            throw new BusinessException("Cet étudiant est déjà inscrit à ce module.");
        }

        student.getModules().add(module);
        userRepository.save(student);
    }

    @Transactional
    public void assignTeacher(Long moduleId, Long teacherId, String referenceTeacherLogin) {
        Module module = moduleRepository.findWithStudentsById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module introuvable : " + moduleId));

        if (!isReferenceTeacher(module, referenceTeacherLogin)) {
            throw new BusinessException("Seul le professeur référent peut ajouter des professeurs à ce module.");
        }

        User teacherToAdd = userRepository.findWithModulesById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Professeur introuvable : " + teacherId));

        if (teacherToAdd.getRole() != Role.TEACHER) {
            throw new BusinessException("L'utilisateur sélectionné n'est pas un professeur.");
        }

        boolean alreadyAssigned = teacherToAdd.getModules().stream()
                .anyMatch(m -> m.getId().equals(moduleId));
        if (alreadyAssigned) {
            throw new BusinessException("Ce professeur est déjà associé à ce module.");
        }

        teacherToAdd.getModules().add(module);
        userRepository.save(teacherToAdd);
    }

    @Transactional(readOnly = true)
    public List<ModuleSummary> getModulesForStudent(String studentLogin) {
        User student = userRepository.findModulesByLogin(studentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé : " + studentLogin));
        return student.getModules().stream()
                .sorted(Comparator.comparing(Module::getName))
                .map(ModuleSummary::from)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<ModuleSummary> searchModulesByName(String name) {
        if (name == null || name.isBlank()) {
            return getAllModulesSummary();
        }
        return moduleRepository.findByNameContainingIgnoreCaseOrderByName(name.trim())
                .stream()
                .map(ModuleSummary::from)
                .toList();
    }

    /**
     * Creates a module with its reference teacher.
     *
     * @throws BusinessException         if the name is blank or already used, or if
     *                                   the reference teacher is missing or is not
     *                                   a teacher
     * @throws ResourceNotFoundException if the reference teacher does not exist
     */
    @Transactional
    public void createModule(String name, Long referenceTeacherId) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("The module name is required.");
        }
        if (referenceTeacherId == null) {
            throw new BusinessException("The reference teacher is required.");
        }
        String trimmedName = name.trim();
        if (moduleRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BusinessException("A module with this name already exists.");
        }

        User referenceTeacher = userRepository.findById(referenceTeacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + referenceTeacherId));
        if (referenceTeacher.getRole() != Role.TEACHER) {
            throw new BusinessException("The selected user is not a teacher.");
        }

        Module module = new Module();
        module.setName(trimmedName);
        module.setReferenceTeacher(referenceTeacher);
        moduleRepository.save(module);
    }

    /**
     * Returns the module's reference teacher with the teachers it can be chosen among,
     * and every teacher and student flagged with whether they are enrolled in the module.
     * The reference teacher is left out of the teachers checkboxes: it is chosen separately.
     *
     * @throws ResourceNotFoundException if the module does not exist
     */
    @Transactional(readOnly = true)
    public ModuleMembers getModuleMembers(Long moduleId) {
        Module module = moduleRepository.findWithStudentsById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + moduleId));
        User reference = module.getReferenceTeacher();

        Set<Long> enrolledIds = module.getEnrolledUsers().stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        List<User> allTeachers = userRepository.findByRole(Role.TEACHER).stream()
                .sorted(BY_NAME)
                .toList();
        List<ModuleMembers.Member> teachers = allTeachers.stream()
                .filter(teacher -> reference == null || !teacher.getId().equals(reference.getId()))
                .map(teacher -> new ModuleMembers.Member(UserSummary.from(teacher), enrolledIds.contains(teacher.getId())))
                .toList();
        List<ModuleMembers.Member> students = userRepository.findByRole(Role.STUDENT).stream()
                .sorted(BY_NAME)
                .map(student -> new ModuleMembers.Member(UserSummary.from(student), enrolledIds.contains(student.getId())))
                .toList();

        return new ModuleMembers(
                module.getId(),
                module.getName(),
                reference != null ? reference.getId() : null,
                allTeachers.stream().map(UserSummary::from).toList(),
                teachers,
                students);
    }

    /**
     * Sets the reference teacher of a module and the teachers and students enrolled in it:
     * the selected users are enrolled, the current members who are not selected anymore are unenrolled.
     * The reference teacher is not enrolled as a regular teacher. The absences of an unenrolled student are kept.
     *
     * @param moduleId           the module's id
     * @param referenceTeacherId the new reference teacher of the module
     * @param teacherIds         the teachers that must be enrolled (the reference teacher is ignored)
     * @param studentIds         the students that must be enrolled
     * @throws ResourceNotFoundException if the module or one of the users does not exist
     * @throws BusinessException         if the reference teacher is missing or not a teacher,
     *                                   or if a selected teacher is not a teacher or a selected student not a student
     */
    @Transactional
    public void updateMembers(Long moduleId, Long referenceTeacherId,
            Collection<Long> teacherIds, Collection<Long> studentIds) {
        Module module = moduleRepository.findWithStudentsById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + moduleId));

        if (referenceTeacherId == null) {
            throw new BusinessException("The reference teacher is required.");
        }
        User reference = userRepository.findById(referenceTeacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + referenceTeacherId));
        if (reference.getRole() != Role.TEACHER) {
            throw new BusinessException("The selected user is not a teacher.");
        }

        Set<Long> wantedTeacherIds = teacherIds == null ? new HashSet<>() : new HashSet<>(teacherIds);
        Set<Long> wantedStudentIds = studentIds == null ? new HashSet<>() : new HashSet<>(studentIds);
        wantedTeacherIds.remove(referenceTeacherId);

        List<User> wantedUsers = new ArrayList<>();
        wantedUsers.addAll(findUsersWithRole(wantedTeacherIds, Role.TEACHER,
                "The selected user is not a teacher."));
        wantedUsers.addAll(findUsersWithRole(wantedStudentIds, Role.STUDENT,
                "The selected user is not a student."));

        // User owns the enrollments join table: (un)link from that side
        for (User member : new ArrayList<>(module.getEnrolledUsers())) {
            boolean unselectedTeacher = member.getRole() == Role.TEACHER && !wantedTeacherIds.contains(member.getId());
            boolean unselectedStudent = member.getRole() == Role.STUDENT && !wantedStudentIds.contains(member.getId());
            if (unselectedTeacher || unselectedStudent) {
                member.getModules().removeIf(m -> m.getId().equals(moduleId));
                module.getEnrolledUsers().remove(member);
                userRepository.save(member);
            }
        }
        for (User user : wantedUsers) {
            boolean alreadyEnrolled = user.getModules().stream().anyMatch(m -> m.getId().equals(moduleId));
            if (!alreadyEnrolled) {
                user.getModules().add(module);
                module.getEnrolledUsers().add(user);
                userRepository.save(user);
            }
        }
        module.setReferenceTeacher(reference);
        moduleRepository.save(module);
    }

    private List<User> findUsersWithRole(Set<Long> ids, Role role, String wrongRoleMessage) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<User> users = userRepository.findAllById(ids);
        if (users.size() != ids.size()) {
            throw new ResourceNotFoundException("User not found.");
        }
        if (users.stream().anyMatch(user -> user.getRole() != role)) {
            throw new BusinessException(wrongRoleMessage);
        }
        return users;
    }

    /**
     * Deletes a module, along with its enrollments and its absences.
     *
     * @throws ResourceNotFoundException if the module does not exist
     */
    @Transactional
    public void deleteModule(Long id) {
        Module module = moduleRepository.findWithStudentsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + id));

        // User owns the enrollments join table: unlink from that side
        for (User user : module.getEnrolledUsers()) {
            user.getModules().remove(module);
        }
        absenceRepository.deleteByModuleId(id);
        moduleRepository.delete(module);
    }

}
