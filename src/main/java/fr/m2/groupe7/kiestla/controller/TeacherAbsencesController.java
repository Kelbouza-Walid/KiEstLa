package fr.m2.groupe7.kiestla.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.m2.groupe7.kiestla.service.AbsenteeService.SortBy;
import fr.m2.groupe7.kiestla.service.NotificationService;
import fr.m2.groupe7.kiestla.service.TeacherAbsenceService;

@Controller
public class TeacherAbsencesController {

    private final TeacherAbsenceService teacherAbsenceService;
    private final NotificationService notificationService;

    public TeacherAbsencesController(TeacherAbsenceService teacherAbsenceService,
                                     NotificationService notificationService) {
        this.teacherAbsenceService = teacherAbsenceService;
        this.notificationService = notificationService;
    }

    @GetMapping("/teacher/absences")
    public String absences(@RequestParam(defaultValue = "MODULE") SortBy sort,
                           Authentication authentication, Model model) {
        model.addAttribute("absences",
                teacherAbsenceService.getAbsencesForTeacher(authentication.getName(), sort));
        model.addAttribute("sort", sort.name());
        if (authentication != null) {
            model.addAttribute("notifications", notificationService.getNotificationsForUser(authentication.getName()));
            model.addAttribute("unreadNotificationsCount", notificationService.getUnreadCountForUser(authentication.getName()));
        }
        return "teacher-absences";
    }
}