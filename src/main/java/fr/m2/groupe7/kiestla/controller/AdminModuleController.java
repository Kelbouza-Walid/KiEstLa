package fr.m2.groupe7.kiestla.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.m2.groupe7.kiestla.service.ModuleService;
import fr.m2.groupe7.kiestla.service.UserService;

@Controller
@RequestMapping("/admin/modules")
public class AdminModuleController {

    private final ModuleService moduleService;
    private final UserService userService;

    public AdminModuleController(ModuleService moduleService, UserService userService) {
        this.moduleService = moduleService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String name, Model model) {
        model.addAttribute("modules", moduleService.searchModulesByName(name));
        model.addAttribute("name", name);
        model.addAttribute("teachers", userService.getAllTeachers());
        return "admin-modules";
    }

    @PostMapping
    public String create(@RequestParam(required = false) String name,
            @RequestParam(required = false) Long referenceTeacherId,
            RedirectAttributes redirectAttributes) {
        moduleService.createModule(name, referenceTeacherId);
        redirectAttributes.addFlashAttribute("successMessage", "The module has been successfully created.");
        return "redirect:/admin/modules";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        moduleService.deleteModule(id);
        redirectAttributes.addFlashAttribute("successMessage", "The module has been successfully deleted.");
        return "redirect:/admin/modules";
    }

    @GetMapping("/{id}/members")
    public String members(@PathVariable Long id, Model model) {
        model.addAttribute("module", moduleService.getModuleMembers(id));
        return "admin-module-members";
    }

    @PostMapping("/{id}/members")
    public String updateMembers(@PathVariable Long id,
            @RequestParam(required = false) List<Long> teacherIds,
            @RequestParam(required = false) List<Long> studentIds,
            RedirectAttributes redirectAttributes) {
        moduleService.updateMembers(id, teacherIds, studentIds);
        redirectAttributes.addFlashAttribute("successMessage", "Les membres du module ont été mis à jour.");
        return "redirect:/admin/modules/" + id + "/members";
    }
}