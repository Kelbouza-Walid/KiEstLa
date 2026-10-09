package fr.m2.groupe7.kiestla.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
    public String index(Authentication authentication, Model model) {
        String studentLogin = authentication.getName();

        model.addAttribute("modules", moduleService.getModulesForStudent(studentLogin));
        model.addAttribute("absences", absenceService.getAbsencesForStudent(studentLogin));
        return "student";
    }
}