package fr.m2.groupe7.kiestla.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.m2.groupe7.kiestla.service.AbsenteeService;
import fr.m2.groupe7.kiestla.service.AbsenteeService.SortBy;

@Controller
public class TeacherAbsenteeController {

    private final AbsenteeService absenteeService;

    public TeacherAbsenteeController(AbsenteeService absenteeService) {
        this.absenteeService = absenteeService;
    }

    @GetMapping("/teacher/absentees")
    public String absentees(@RequestParam(defaultValue = "MODULE") SortBy sort,
                            Authentication authentication, Model model) {
        model.addAttribute("absentees",
                absenteeService.getAbsenteesForTeacher(authentication.getName(), sort));
        model.addAttribute("sort", sort.name());
        return "absentees";
    }
}