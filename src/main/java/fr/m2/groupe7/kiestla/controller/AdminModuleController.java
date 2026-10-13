package fr.m2.groupe7.kiestla.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fr.m2.groupe7.kiestla.service.ModuleService;

@Controller
@RequestMapping("/admin/modules")
public class AdminModuleController {

    private final ModuleService moduleService;

    public AdminModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String name, Model model) {
        model.addAttribute("modules", moduleService.searchModulesByName(name));
        model.addAttribute("name", name);
        return "admin-modules";
    }
}