package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.ModuleRepository;
import fr.m2.groupe7.kiestla.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class TeacherController {

    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;

    public TeacherController(UserRepository userRepository, ModuleRepository moduleRepository) {
        this.userRepository = userRepository;
        this.moduleRepository = moduleRepository;
    }

    @GetMapping("/teacher")
    public String teacherPage(Authentication authentication, Model model) {
        User teacher = userRepository.findModulesByLogin(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé : " + authentication.getName()));
        model.addAttribute("modules", teacher.getModules());
        return "teacher";
    }

    @GetMapping("/teacher/modules/{id}")
    public String moduleParticipants(@PathVariable Long id, Authentication authentication, Model model) {
        User teacher = userRepository.findModulesByLogin(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé : " + authentication.getName()));

        boolean assignedToTeacher = teacher.getModules().stream()
                .anyMatch(module -> module.getId().equals(id));
        if (!assignedToTeacher) {
            return "redirect:/teacher";
        }

        Module module = moduleRepository.findWithStudentsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module introuvable : " + id));
        model.addAttribute("module", module);
        model.addAttribute("teachers", module.getStudents().stream()
                .filter(user -> user.getRole() == Role.TEACHER)
                .toList());
        model.addAttribute("students", module.getStudents().stream()
                .filter(user -> user.getRole() == Role.STUDENT)
                .toList());
        Set<Long> enrolledStudentIds = module.getStudents().stream()
                .filter(user -> user.getRole() == Role.STUDENT)
                .map(User::getId)
                .collect(Collectors.toSet());
        model.addAttribute("availableStudents", userRepository.findByRole(Role.STUDENT).stream()
                .filter(student -> !enrolledStudentIds.contains(student.getId()))
                .toList());

        return "teacher-module";
    }

    @PostMapping("/teacher/modules/{id}/students")
    @Transactional
    public String addStudentToModule(
            @PathVariable Long id,
            @RequestParam Long studentId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User teacher = userRepository.findModulesByLogin(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé : " + authentication.getName()));

        boolean assignedToTeacher = teacher.getModules().stream()
                .anyMatch(module -> module.getId().equals(id));
        if (!assignedToTeacher) {
            return "redirect:/teacher";
        }

        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module introuvable : " + id));
        User student = userRepository.findWithModulesById(studentId).orElse(null);
        if (student == null || student.getRole() != Role.STUDENT) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "L’utilisateur sélectionné n’est pas un étudiant.");
            return "redirect:/teacher/modules/" + id;
        }

        boolean alreadyEnrolled = student.getModules().stream()
                .anyMatch(enrolledModule -> enrolledModule.getId().equals(id));
        if (alreadyEnrolled) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Cet étudiant est déjà inscrit à ce module.");
            return "redirect:/teacher/modules/" + id;
        }

        student.getModules().add(module);
        userRepository.save(student);
        redirectAttributes.addFlashAttribute(
                "successMessage", "L’étudiant a été ajouté au module.");
        return "redirect:/teacher/modules/" + id;
    }
}