package fr.m2.groupe7.kiestla.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * AdminController
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    /**
     * Displays the admin dashboard.
     *
     * @return the name of the Thymeleaf template to render
     */
    @GetMapping
    public String index() {
        return "admin";
    }
}
