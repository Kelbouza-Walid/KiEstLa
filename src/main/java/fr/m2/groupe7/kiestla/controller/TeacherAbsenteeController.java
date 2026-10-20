package fr.m2.groupe7.kiestla.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.m2.groupe7.kiestla.service.AbsenteeService;
import fr.m2.groupe7.kiestla.service.AbsenteeService.SortBy;
import fr.m2.groupe7.kiestla.service.NotificationService;

@Controller
public class TeacherAbsenteeController {

    private final AbsenteeService absenteeService;
    private final NotificationService notificationService;

    public TeacherAbsenteeController(AbsenteeService absenteeService,
                                     NotificationService notificationService) {
        this.absenteeService = absenteeService;
        this.notificationService = notificationService;
    }

    @GetMapping("/teacher/absentees")
    public String absentees(@RequestParam(defaultValue = "MODULE") SortBy sort,
                            Authentication authentication, Model model) {
        model.addAttribute("absentees",
                absenteeService.getAbsenteesForTeacher(authentication.getName(), sort));
        model.addAttribute("sort", sort.name());
        if (authentication != null) {
            model.addAttribute("notifications", notificationService.getNotificationsForUser(authentication.getName()));
            model.addAttribute("unreadNotificationsCount", notificationService.getUnreadCountForUser(authentication.getName()));
        }
        return "absentees";
    }
}