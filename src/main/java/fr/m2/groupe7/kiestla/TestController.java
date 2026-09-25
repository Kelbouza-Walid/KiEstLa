package fr.m2.groupe7.kiestla;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class TestController {

    @GetMapping("/")
    public String hello() {
        return "redirect:/secretary";
    }
}
