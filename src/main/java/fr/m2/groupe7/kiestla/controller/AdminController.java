package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.service.UserService;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * AdminController
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Displays the admin dashboard.
     *
     * @return the name of the Thymeleaf template to render
     */
    @GetMapping
    public String index(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin";
    }
}
