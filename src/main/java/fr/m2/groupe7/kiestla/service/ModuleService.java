package fr.m2.groupe7.kiestla.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import fr.m2.groupe7.kiestla.dto.ModuleSummary;
import fr.m2.groupe7.kiestla.dto.ModuleDetails;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;

@Service
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;

    public ModuleService(ModuleRepository moduleRepository, UserRepository userRepository) {
        this.moduleRepository = moduleRepository;
        this.userRepository = userRepository;
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

}
