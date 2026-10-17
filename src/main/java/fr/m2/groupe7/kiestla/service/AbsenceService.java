package fr.m2.groupe7.kiestla.service;

import fr.m2.groupe7.kiestla.dto.AbsenceSummary;
import fr.m2.groupe7.kiestla.dto.JustifyAbsenceRequest;
import fr.m2.groupe7.kiestla.dto.ModuleDetails;
import fr.m2.groupe7.kiestla.dto.StudentAbsenceSummary;
import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.exception.ResourceNotFoundException;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final ModuleService moduleService;

    public AbsenceService(AbsenceRepository absenceRepository,
            ModuleRepository moduleRepository,
            UserRepository userRepository,
            ModuleService moduleService) {
        this.absenceRepository = absenceRepository;
        this.moduleRepository = moduleRepository;
        this.userRepository = userRepository;
        this.moduleService = moduleService;
    }

    /**
     * Retrieves all absences rows
     */
    public List<AbsenceSummary> getAbsences() {
        return absenceRepository.findAllByOrderByDateDesc()
                .stream()
                .map(AbsenceSummary::from)
                .toList();
    }

    /**
     * Retrieves all absences for a module of id id
     */
    public List<AbsenceSummary> getAbsencesForModule(Long id) {
        return absenceRepository.findByModuleId(id)
                .stream()
                .map(AbsenceSummary::from)
                .toList();
    }

    /**
     * Adds a motive (title and description) to an absence and marks its status as
     * JUSTIFIED.
     */
    @Transactional
    public void justifyAbsence(Long id, JustifyAbsenceRequest justificationReq) {
        Absence absence = absenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Absence introuvable avec l'ID : " + id));

        absence.setMotiveTitle(justificationReq.motiveTitle());
        absence.setMotiveDescription(justificationReq.motiveDescription());
        absence.setStatus(AbsenceStatus.JUSTIFIED);
    }

    /**
     * Removes the motive from an absence and reverts its status to
     * UNJUSTIFIED.
     */
    @Transactional
    public void unjustifyAbsence(Long id) {
        Absence absence = absenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Absence introuvable avec l'ID : " + id));

        absence.setMotiveTitle(null);
        absence.setMotiveDescription(null);
        absence.setStatus(AbsenceStatus.UNJUSTIFIED);
    }

    /**
     * add unjustfied absence for student in module
     * 
     * @throws ResourceNotFoundException if the module or the teacher or the student
     *                                   dont exsist
     * @throws BusinessException         if the teacher dont have access to the
     *                                   module
     *
     */
    @Transactional
    public void addAbsence(Long moduleId, Long studentId, LocalDate date, String teacherLogin) {
        // Throws if the teacher has no access to this module
        ModuleDetails module = moduleService.getModuleDetailsForTeacher(moduleId, teacherLogin);

        boolean enrolled = module.students().stream()
                .anyMatch(s -> s.id().equals(studentId));
        if (!enrolled) {
            throw new BusinessException("cet étudiant n'est pas inscris au ce module");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("étudiant introuvable : " + studentId));
        Module moduleEntity = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("module introuvable : " + moduleId));

        Absence absence = new Absence();
        absence.setStudent(student);
        absence.setModule(moduleEntity);
        absence.setDate(date);
        absenceRepository.save(absence);
    }

    @Transactional(readOnly = true)
    public List<StudentAbsenceSummary> getAbsencesForStudent(String studentLogin) {
        return absenceRepository.findByStudentLoginOrderByDateDesc(studentLogin)
                .stream()
                .map(StudentAbsenceSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentAbsenceSummary getAbsenceForStudent(Long absenceId, String studentLogin) {
        Absence absence = absenceRepository.findByIdAndStudentLogin(absenceId, studentLogin)
                .orElseThrow(() -> new ResourceNotFoundException("Absence introuvable."));
        return StudentAbsenceSummary.from(absence);
    }
}
