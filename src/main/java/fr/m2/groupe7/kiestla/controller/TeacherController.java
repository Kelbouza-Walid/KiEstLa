package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.service.AbsenceService;
import fr.m2.groupe7.kiestla.service.ModuleService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class TeacherController {

    private final ModuleService moduleService;
    private final AbsenceService absenceService;

    public TeacherController(
            ModuleService moduleService,
            AbsenceService absenceService) {
        this.moduleService = moduleService;
        this.absenceService = absenceService;
    }

    @GetMapping("/teacher")
    public String teacherPage(Authentication authentication, Model model) {
        model.addAttribute("modules", moduleService.getModulesForTeacher(authentication.getName()));
        return "teacher";
    }

    @PostMapping("/teacher/modules/{id}/students")
    public String addStudentToModule(
            @PathVariable Long id,
            @RequestParam Long studentId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        moduleService.enrollStudent(id, studentId, authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", "L’étudiant a été ajouté au module.");
        return "redirect:/module-details/" + id;
    }

    @PostMapping("/teacher/modules/{id}/teachers")
    public String addTeacherToModule(
            @PathVariable Long id,
            @RequestParam Long teacherId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        moduleService.assignTeacher(id, teacherId, authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Le professeur a été ajouté au module.");
        return "redirect:/module-details/" + id;
    }

    @PostMapping("/teacher/modules/{id}/absences")
    public String addAbsence(
            @PathVariable Long id,
            @RequestParam Long absentStudentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        absenceService.addAbsence(id, absentStudentId, date, authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", "L'absence a été ajoutée.");
        return "redirect:/module-details/" + id;
    }
}
