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

/**
 * Secretary Page controller
 */
@Controller
@RequestMapping("/secretary")
public class SecretaryController {

    private final AbsenceService absenceService;
    private final ModuleService moduleService;
    private final AbsenteeService absenteeService;

    public SecretaryController(AbsenceService absenceService, ModuleService moduleService,
                               AbsenteeService absenteeService) {
        this.absenceService = absenceService;
        this.moduleService = moduleService;
        this.absenteeService = absenteeService;
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
                        Model model) {
        List<AbsenceSummary> absences = absenceService.getAbsences();
        List<ModuleSummary> modules = moduleService.getAllModulesSummary();

        model.addAttribute("absences", absences);
        model.addAttribute("modules", modules);
        model.addAttribute("absentees", absenteeService.getAbsenteesForSecretary(moduleId, student, sort));
        model.addAttribute("selectedModuleId", moduleId);
        model.addAttribute("studentQuery", student);
        model.addAttribute("sort", sort.name());
        return "secretary";
    }
}
