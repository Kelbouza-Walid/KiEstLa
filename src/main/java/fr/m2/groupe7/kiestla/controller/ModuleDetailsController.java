package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.service.AbsenceService;
import fr.m2.groupe7.kiestla.service.ModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/module-details")
public class ModuleDetailsController {

    /**
     * ModuleDetails Page controller
     */

    @Autowired
    private AbsenceService absenceService;

    @Autowired
    private ModuleService moduleService;

    /**
     * Displays the ModuleDetails page.
     *
     *
     * @return the name of the Thymeleaf template to render
     */
    @GetMapping("/{id}")
    public String index(Model model, @PathVariable Long id) {

        List<Absence> absences = absenceService.getAbsencesWithID(id);
        Optional<Module> module = moduleService.getModuleById(id);
        if (module.isEmpty()){
            return "forward:/404.html";
        }
        Module mod = module.get();
        model.addAttribute("absences", absences);
        model.addAttribute("module", mod);
        return "module-details";
    }
}
