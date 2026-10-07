package fr.m2.groupe7.kiestla.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public SecretaryController(AbsenceService absenceService, ModuleService moduleService) {
        this.absenceService = absenceService;
        this.moduleService = moduleService;
    }

    @PostMapping("/absence/{id}/justify")
    public String justifyAbsence(
            @PathVariable Long id,
            @Valid @ModelAttribute JustifyAbsenceRequest justificationReq,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "La justification est invalide (vide ou trop longue).");
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
    public String index(Model model) {
        List<AbsenceSummary> absences = absenceService.getAbsences();

        List<ModuleSummary> modules = moduleService.getAllModulesSummary();

        model.addAttribute("absences", absences);
        model.addAttribute("modules", modules);
        return "secretary";
    }
}
