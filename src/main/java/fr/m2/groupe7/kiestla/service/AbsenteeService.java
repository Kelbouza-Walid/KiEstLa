package fr.m2.groupe7.kiestla.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.m2.groupe7.kiestla.dto.AbsenteeSummary;
import fr.m2.groupe7.kiestla.dto.ModuleSummary;
import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;

/**
 * Lists the students who reached the threshold of UNJUSTIFIED absences
 * in one of the modules of a teacher.
 */
@Service
public class AbsenteeService {

    public static final int ABSENCE_THRESHOLD = 3;

    public enum SortBy {
        MODULE, STUDENT
    }

    private final AbsenceRepository absenceRepository;
    private final ModuleService moduleService;

    public AbsenteeService(AbsenceRepository absenceRepository, ModuleService moduleService) {
        this.absenceRepository = absenceRepository;
        this.moduleService = moduleService;
    }

    @Transactional(readOnly = true)
    public List<AbsenteeSummary> getAbsenteesForTeacher(String teacherLogin, SortBy sortBy) {
        List<AbsenteeSummary> absentees = new ArrayList<>();

        for (ModuleSummary module : moduleService.getModulesForTeacher(teacherLogin)) {
            Map<Long, List<Absence>> unjustifiedByStudent = absenceRepository.findByModuleId(module.id())
                    .stream()
                    .filter(absence -> absence.getStatus() == AbsenceStatus.UNJUSTIFIED)
                    .collect(Collectors.groupingBy(absence -> absence.getStudent().getId()));

            for (List<Absence> studentAbsences : unjustifiedByStudent.values()) {
                if (studentAbsences.size() >= ABSENCE_THRESHOLD) {
                    User student = studentAbsences.get(0).getStudent();
                    absentees.add(new AbsenteeSummary(
                            student.getId(),
                            student.getFirstName(),
                            student.getLastName(),
                            module.id(),
                            module.name(),
                            studentAbsences.size()));
                }
            }
        }

        absentees.sort(comparatorFor(sortBy));
        return absentees;
    }

    private Comparator<AbsenteeSummary> comparatorFor(SortBy sortBy) {
        Comparator<AbsenteeSummary> byModule = Comparator.comparing(AbsenteeSummary::moduleName);
        Comparator<AbsenteeSummary> byStudent = Comparator.comparing(AbsenteeSummary::lastName)
                .thenComparing(AbsenteeSummary::firstName);

        return sortBy == SortBy.STUDENT
                ? byStudent.thenComparing(byModule)
                : byModule.thenComparing(byStudent);
    }
}