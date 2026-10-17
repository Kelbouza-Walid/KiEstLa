package fr.m2.groupe7.kiestla.controller;

import java.util.Comparator;
import java.util.List;

import fr.m2.groupe7.kiestla.dto.StudentAbsenceSummary;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import fr.m2.groupe7.kiestla.service.AbsenceService;
import fr.m2.groupe7.kiestla.service.ModuleService;

/**
 * Student Page controller
 */
@Controller
@RequestMapping("/student")
public class StudentController {

    private final ModuleService moduleService;
    private final AbsenceService absenceService;

    public StudentController(ModuleService moduleService, AbsenceService absenceService) {
        this.moduleService = moduleService;
        this.absenceService = absenceService;
    }

    @GetMapping
    public String index(Authentication authentication,
                        @RequestParam(required = false) String module,
                        @RequestParam(defaultValue = "DATE") String sortBy,
                        Model model) {
        String studentLogin = authentication.getName();
        List<StudentAbsenceSummary> absences = absenceService.getAbsencesForStudent(studentLogin);
        if (module != null && !module.isBlank()) {
            absences = absences.stream()
                    .filter(absence -> absence.moduleName().equals(module))
                    .toList();
        }
        if ("MODULE".equals(sortBy)) {
            absences = absences.stream()
                    .sorted(Comparator.comparing(StudentAbsenceSummary::moduleName)
                            .thenComparing(StudentAbsenceSummary::date))
                    .toList();
        }

        model.addAttribute("modules", moduleService.getModulesForStudent(studentLogin));
        model.addAttribute("absences", absences);
        model.addAttribute("selectedModule", module);
        model.addAttribute("sortBy", sortBy);
        return "student";
    }

    @GetMapping("/absences")
    public String absences(Authentication authentication, Model model) {
        model.addAttribute("absences", absenceService.getAbsencesForStudent(authentication.getName()));
        return "student-absences";
    }

    @GetMapping("/absences/{id}")
    public String absenceDetails(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("absence", absenceService.getAbsenceForStudent(id, authentication.getName()));
        return "student-absence-details";
    }
}