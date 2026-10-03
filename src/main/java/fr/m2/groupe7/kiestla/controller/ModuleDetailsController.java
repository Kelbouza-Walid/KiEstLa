package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.service.AbsenceService;
import fr.m2.groupe7.kiestla.service.ModuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * ModuleDetails Page controller
 */
@Controller
@RequestMapping("/module-details")
public class ModuleDetailsController {

    private final AbsenceService absenceService;
    private final ModuleService moduleService;

    public ModuleDetailsController(AbsenceService absenceService, ModuleService moduleService) {
        this.absenceService = absenceService;
        this.moduleService = moduleService;
    }

    /**
     * Displays the ModuleDetails page.
     * Not-found cases are handled by GlobalExceptionHandler.
     *
     * @return the name of the Thymeleaf template to render
     */
    @GetMapping("/{id}")
    public String index(@PathVariable Long id, Model model) {
        Module module = moduleService.getModuleWithStudentsById(id);
        List<Absence> absences = absenceService.getAbsencesWithID(id);

        model.addAttribute("module", module);
        model.addAttribute("absences", absences);
        return "module-details";
    }
}
