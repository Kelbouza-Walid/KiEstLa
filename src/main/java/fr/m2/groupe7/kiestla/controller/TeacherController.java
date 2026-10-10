package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.dto.ModuleDetails;
import fr.m2.groupe7.kiestla.service.AbsenceService;
import fr.m2.groupe7.kiestla.service.ModuleService;
import fr.m2.groupe7.kiestla.service.UserService;

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

    private final UserService userService;
    private final ModuleService moduleService;
    private final AbsenceService absenceService;

    public TeacherController(
            UserService userService,
            ModuleService moduleService,
            AbsenceService absenceService) {
        this.userService = userService;
        this.moduleService = moduleService;
        this.absenceService = absenceService;
    }

    @GetMapping("/teacher")
    public String teacherPage(Authentication authentication, Model model) {
        model.addAttribute("modules", moduleService.getModulesForTeacher(authentication.getName()));
        return "teacher";
    }

    @GetMapping("/teacher/modules/{id}")
    public String moduleParticipants(@PathVariable Long id, Authentication authentication, Model model) {
        String teacherLogin = authentication.getName();

        ModuleDetails module = moduleService.getModuleDetailsForTeacher(id, teacherLogin);
        boolean isReferenceTeacher = moduleService.isReferenceTeacher(id, teacherLogin);

        model.addAttribute("module", module);
        model.addAttribute("absences", absenceService.getAbsencesForModule(id));
        model.addAttribute("availableStudents", userService.getAvailableStudentsForModule(module));

        model.addAttribute("isReferenceTeacher", isReferenceTeacher);
        if (isReferenceTeacher) {
            model.addAttribute("availableTeachers", userService.getAvailableTeachersForModule(module));
        }

        return "teacher-module";

    }

    @PostMapping("/teacher/modules/{id}/students")
    public String addStudentToModule(
            @PathVariable Long id,
            @RequestParam Long studentId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        moduleService.enrollStudent(id, studentId, authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", "L’étudiant a été ajouté au module.");
        return "redirect:/teacher/modules/" + id;
    }

    @PostMapping("/teacher/modules/{id}/teachers")
    public String addTeacherToModule(
            @PathVariable Long id,
            @RequestParam Long teacherId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        moduleService.assignTeacher(id, teacherId, authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Le professeur a été ajouté au module.");
        return "redirect:/teacher/modules/" + id;
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
        return "redirect:/teacher/modules/" + id;
    }
}
