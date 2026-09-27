package fr.m2.groupe7.kiestla.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/secretary";
    }

    @GetMapping("/hello")
    @ResponseBody
    public String hello() {
        return "<h1>Ça marche et le Groupe 7 est en ligne</h1>";
    }
}
