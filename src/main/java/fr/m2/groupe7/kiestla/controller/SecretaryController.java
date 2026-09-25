package fr.m2.groupe7.kiestla.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Sort;

import fr.m2.groupe7.kiestla.entity.Absence;

/**
 * Secretary Page controller
 */
@Controller
@RequestMapping("/secretary") // TODO:change this to homepage when roles are made
public class SecretaryController {

    @Autowired
    private fr.m2.groupe7.kiestla.service.AbsenceService absenceService;

    // TODO: remove this once real module is implemented
    public record FakeModule(String name, String referentTeacherName) {
    }

    @PostMapping("/absence/{id}/justify")
    public String justifyAbsence(@PathVariable Long id, @RequestParam String justificationText) {
        absenceService.justifyAbsence(id, justificationText);
        return "redirect:/secretary";
    }

    @PostMapping("/absence/{id}/unjustify")
    public String unjustifyAbsence(@PathVariable Long id) {
        absenceService.unjustifyAbsence(id);
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
        List<Absence> absences = absenceService.getAllAbsencesSortedByDateDesc();

        List<FakeModule> modules = List.of(
                new FakeModule("Fonctionnal Testing", "Mr Bouquet"),
                new FakeModule("Horrible course", "Horrible teacher"));

        model.addAttribute("absences", absences);
        model.addAttribute("modules", modules);
        return "secretary";
    }
}
