package fr.m2.groupe7.kiestla.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.m2.groupe7.kiestla.service.AbsenteeService.SortBy;
import fr.m2.groupe7.kiestla.service.TeacherAbsenceService;

@Controller
public class TeacherAbsencesController {

    private final TeacherAbsenceService teacherAbsenceService;

    public TeacherAbsencesController(TeacherAbsenceService teacherAbsenceService) {
        this.teacherAbsenceService = teacherAbsenceService;
    }

    @GetMapping("/teacher/absences")
    public String absences(@RequestParam(defaultValue = "MODULE") SortBy sort,
                           Authentication authentication, Model model) {
        model.addAttribute("absences",
                teacherAbsenceService.getAbsencesForTeacher(authentication.getName(), sort));
        model.addAttribute("sort", sort.name());
        return "teacher-absences";
    }
}