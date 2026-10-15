package fr.m2.groupe7.kiestla.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.m2.groupe7.kiestla.dto.ModuleSummary;
import fr.m2.groupe7.kiestla.dto.TeacherAbsenceRow;
import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
import fr.m2.groupe7.kiestla.service.AbsenteeService.SortBy;

/**
 * Global list of the absences of all the modules of a teacher.
 */
@Service
public class TeacherAbsenceService {

    private final AbsenceRepository absenceRepository;
    private final ModuleService moduleService;

    public TeacherAbsenceService(AbsenceRepository absenceRepository, ModuleService moduleService) {
        this.absenceRepository = absenceRepository;
        this.moduleService = moduleService;
    }

    @Transactional(readOnly = true)
    public List<TeacherAbsenceRow> getAbsencesForTeacher(String teacherLogin, SortBy sortBy) {
        List<TeacherAbsenceRow> rows = new ArrayList<>();

        for (ModuleSummary module : moduleService.getModulesForTeacher(teacherLogin)) {
            for (Absence absence : absenceRepository.findByModuleId(module.id())) {
                rows.add(new TeacherAbsenceRow(
                        absence.getId(),
                        module.name(),
                        absence.getStudent().getFirstName(),
                        absence.getStudent().getLastName(),
                        absence.getDate(),
                        absence.getStatus(),
                        absence.getMotiveTitle(),
                        absence.getMotiveDescription()));
            }
        }

        rows.sort(comparatorFor(sortBy));
        return rows;
    }

    private Comparator<TeacherAbsenceRow> comparatorFor(SortBy sortBy) {
        Comparator<TeacherAbsenceRow> byModule = Comparator.comparing(TeacherAbsenceRow::moduleName);
        Comparator<TeacherAbsenceRow> byStudent = Comparator.comparing(TeacherAbsenceRow::lastName)
                .thenComparing(TeacherAbsenceRow::firstName);
        Comparator<TeacherAbsenceRow> byDate = Comparator.comparing(TeacherAbsenceRow::date);

        return sortBy == SortBy.STUDENT
                ? byStudent.thenComparing(byModule).thenComparing(byDate)
                : byModule.thenComparing(byStudent).thenComparing(byDate);
    }
}