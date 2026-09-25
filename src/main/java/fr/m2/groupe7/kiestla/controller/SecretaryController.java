package fr.m2.groupe7.kiestla.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.data.domain.Sort;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;

/**
 * Secretary Page controller
 */
@Controller
@RequestMapping("/secretary") // TODO:change this to homepage when roles are made
public class SecretaryController {

    @Autowired
    private AbsenceRepository absenceRepository;

    // TODO: remove this once real module is implemented
    public record FakeModule(String name, String referentTeacherName) {
    }

    @PostMapping("/absence/{id}/toggle-status")
    public String toggleAbsenceStatus(@PathVariable Long id) {
        Optional<Absence> absenceOpt = absenceRepository.findById(id);
        if (absenceOpt.isPresent()) {
            Absence absence = absenceOpt.get();
            if (absence.getStatus() == AbsenceStatus.UNJUSTIFIED) {
                absence.setStatus(AbsenceStatus.JUSTIFIED);
            } else {
                absence.setStatus(AbsenceStatus.UNJUSTIFIED);
            }

            absenceRepository.save(absence);
        }
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
        List<Absence> absences = absenceRepository.findAll(Sort.by(Sort.Direction.DESC, "date"));

        List<FakeModule> modules = List.of(
                new FakeModule("Fonctionnal Testing", "Mr Bouquet"),
                new FakeModule("Horrible course", "Horrible teacher"));

        model.addAttribute("absences", absences);
        model.addAttribute("modules", modules);
        return "secretary";
    }
}
