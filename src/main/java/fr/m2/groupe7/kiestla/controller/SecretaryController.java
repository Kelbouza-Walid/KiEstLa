package fr.m2.groupe7.kiestla.controller;

import java.util.List;

import fr.m2.groupe7.kiestla.service.AbsenteeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.m2.groupe7.kiestla.dto.AbsenceSummary;
import fr.m2.groupe7.kiestla.dto.JustifyAbsenceRequest;
import fr.m2.groupe7.kiestla.dto.ModuleSummary;
import fr.m2.groupe7.kiestla.service.AbsenceService;
import fr.m2.groupe7.kiestla.service.ModuleService;

import fr.m2.groupe7.kiestla.service.NotificationService;
import org.springframework.security.core.Authentication;

/**
 * Secretary Page controller
 */
@Controller
@RequestMapping("/secretary")
public class SecretaryController {

    private final AbsenceService absenceService;
    private final ModuleService moduleService;
    private final AbsenteeService absenteeService;
    private final NotificationService notificationService;

    public SecretaryController(AbsenceService absenceService, ModuleService moduleService,
                               AbsenteeService absenteeService, NotificationService notificationService) {
        this.absenceService = absenceService;
        this.moduleService = moduleService;
        this.absenteeService = absenteeService;
        this.notificationService = notificationService;
    }

    @PostMapping("/absence/{id}/justify")
    public String justifyAbsence(
            @PathVariable Long id,
            @Valid @ModelAttribute JustifyAbsenceRequest justificationReq,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Le motif est invalide (titre ou description vide ou trop long).");
            return "redirect:/secretary";
        }
        absenceService.justifyAbsence(id, justificationReq);
        redirectAttributes.addFlashAttribute("successMessage", "L'absence a été justifiée avec succès.");
        return "redirect:/secretary";
    }

    @PostMapping("/absence/{id}/unjustify")
    public String unjustifyAbsence(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        absenceService.unjustifyAbsence(id);
        redirectAttributes.addFlashAttribute("successMessage", "La justification de l'absence a été retirée.");
        return "redirect:/secretary";
    }

    /**
     * Displays the secretary page.
     *
     *
     * @return the name of the Thymeleaf template to render
     */
    @GetMapping
    public String index(@RequestParam(required = false) Long moduleId,
                        @RequestParam(required = false) String student,
                        @RequestParam(defaultValue = "MODULE") AbsenteeService.SortBy sort,
                        @RequestParam(required = false) Long absenceModuleId,
                        @RequestParam(required = false) String absenceStudent,
                        Authentication authentication,
                        Model model) {
        List<AbsenceSummary> absences = absenceService.getAbsencesForSecretary(absenceModuleId, absenceStudent);
        List<ModuleSummary> modules = moduleService.getAllModulesSummary();

        if (authentication != null) {
            model.addAttribute("notifications", notificationService.getNotificationsForUser(authentication.getName()));
            model.addAttribute("unreadNotificationsCount", notificationService.getUnreadCountForUser(authentication.getName()));
        }

        model.addAttribute("absences", absences);
        model.addAttribute("modules", modules);
        model.addAttribute("absentees", absenteeService.getAbsenteesForSecretary(moduleId, student, sort));
        model.addAttribute("selectedModuleId", moduleId);
        model.addAttribute("studentQuery", student);
        model.addAttribute("sort", sort.name());
        model.addAttribute("selectedAbsenceModuleId", absenceModuleId);
        model.addAttribute("absenceStudentQuery", absenceStudent);
        return "secretary";
    }

    @PostMapping("/notifications/read-all")
    public String markNotificationsAsRead(
            @RequestHeader(value = "Referer", required = false) String referer,
            Authentication authentication) {
        if (authentication != null) {
            notificationService.markAllAsRead(authentication.getName());
        }
        return (referer != null && !referer.isBlank()) ? "redirect:" + referer : "redirect:/secretary";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationAsRead(
            @PathVariable Long id,
            @RequestHeader(value = "Referer", required = false) String referer,
            Authentication authentication) {
        if (authentication != null) {
            notificationService.markAsRead(id, authentication.getName());
        }
        return (referer != null && !referer.isBlank()) ? "redirect:" + referer : "redirect:/secretary";
    }
}
