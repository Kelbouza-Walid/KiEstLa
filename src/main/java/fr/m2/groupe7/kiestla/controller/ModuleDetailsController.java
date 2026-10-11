package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.dto.AbsenceSummary;
import fr.m2.groupe7.kiestla.dto.ModuleDetails;
import fr.m2.groupe7.kiestla.service.AbsenceService;
import fr.m2.groupe7.kiestla.service.ModuleService;
import fr.m2.groupe7.kiestla.service.UserService;
import org.springframework.security.core.Authentication;
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
    private final UserService userService;

    public ModuleDetailsController(
            AbsenceService absenceService,
            ModuleService moduleService,
            UserService userService) {
        this.absenceService = absenceService;
        this.moduleService = moduleService;
        this.userService = userService;
    }

    /**
     * Displays the ModuleDetails page.
     * Not-found cases are handled by GlobalExceptionHandler.
     *
     * @return the name of the Thymeleaf template to render
     */
    @GetMapping("/{id}")
    public String index(@PathVariable Long id, Authentication authentication, Model model) {
        boolean isTeacher = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_TEACHER"));
        boolean isSecretary = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SECRETARY"));
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STUDENT"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_Admin"));
        ModuleDetails module = isTeacher
                ? moduleService.getModuleDetailsForTeacher(id, authentication.getName())
                : moduleService.getModuleDetails(id);
        boolean isReferenceTeacher = isTeacher
                && moduleService.isReferenceTeacher(id, authentication.getName());
        List<AbsenceSummary> absences = isStudent
                ? List.of()
                : absenceService.getAbsencesForModule(id);

        model.addAttribute("module", module);
        model.addAttribute("absences", absences);
        model.addAttribute("isStudent", isStudent);
        model.addAttribute("isTeacher", isTeacher);
        model.addAttribute("isReferenceTeacher", isReferenceTeacher);
        model.addAttribute("backUrl", isTeacher ? "/teacher" : isSecretary? "/secretary" : isAdmin? "/admin" : "/student");
        if (isTeacher) {
            model.addAttribute("availableStudents", userService.getAvailableStudentsForModule(module));
            if (isReferenceTeacher) {
                model.addAttribute("availableTeachers", userService.getAvailableTeachersForModule(module));
            }
        }
        return "module-details";
    }
}
