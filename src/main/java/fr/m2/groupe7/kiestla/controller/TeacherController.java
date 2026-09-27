package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;
import fr.m2.groupe7.kiestla.repository.AbsenceRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class TeacherController {

    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final AbsenceRepository absenceRepository;

    public TeacherController(
            UserRepository userRepository,
            ModuleRepository moduleRepository,
            AbsenceRepository absenceRepository) {
        this.userRepository = userRepository;
        this.moduleRepository = moduleRepository;
        this.absenceRepository = absenceRepository;
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

        Module module = moduleRepository.findWithStudentsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module introuvable : " + id));
        boolean assignedToTeacher = teacher.getModules().stream()
                .anyMatch(assignedModule -> assignedModule.getId().equals(id));
        boolean isReferenceTeacher = module.getReferenceTeacher() != null
                && module.getReferenceTeacher().getLogin().equals(authentication.getName());
        if (!assignedToTeacher && !isReferenceTeacher) {
            return "redirect:/teacher";
        }

        model.addAttribute("module", module);
        model.addAttribute("absences", absenceRepository.findByModuleIdOrderByDateDesc(id));
        List<User> moduleTeachers = new ArrayList<>(module.getStudents().stream()
                .filter(user -> user.getRole() == Role.TEACHER)
                .toList());
        if (module.getReferenceTeacher() != null
                && moduleTeachers.stream().noneMatch(user -> user.getId().equals(module.getReferenceTeacher().getId()))) {
            moduleTeachers.add(module.getReferenceTeacher());
        }
        model.addAttribute("teachers", moduleTeachers);
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

        model.addAttribute("isReferenceTeacher", isReferenceTeacher);
        if (isReferenceTeacher) {
            Set<Long> enrolledTeacherIds = moduleTeachers.stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());
            model.addAttribute("availableTeachers", userRepository.findByRole(Role.TEACHER).stream()
                    .filter(candidate -> !enrolledTeacherIds.contains(candidate.getId()))
                    .toList());
        }

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

    @PostMapping("/teacher/modules/{id}/teachers")
    @Transactional
    public String addTeacherToModule(
            @PathVariable Long id,
            @RequestParam Long teacherId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Module module = moduleRepository.findWithStudentsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Module introuvable : " + id));

        if (module.getReferenceTeacher() == null
                || !module.getReferenceTeacher().getLogin().equals(authentication.getName())) {
            return "redirect:/teacher";
        }

        User teacherToAdd = userRepository.findWithModulesById(teacherId).orElse(null);
        if (teacherToAdd == null || teacherToAdd.getRole() != Role.TEACHER) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "L’utilisateur sélectionné n’est pas un professeur.");
            return "redirect:/teacher/modules/" + id;
        }

        boolean alreadyAssigned = teacherToAdd.getModules().stream()
                .anyMatch(assignedModule -> assignedModule.getId().equals(id));
        if (alreadyAssigned) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Ce professeur est déjà associé à ce module.");
            return "redirect:/teacher/modules/" + id;
        }

        teacherToAdd.getModules().add(module);
        userRepository.save(teacherToAdd);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Le professeur a été ajouté au module.");
        return "redirect:/teacher/modules/" + id;
    }
}