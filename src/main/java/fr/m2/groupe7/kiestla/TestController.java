package fr.m2.groupe7.kiestla;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    
    @GetMapping("/")
    public String hello() {
        return "<h1>Ça marche ! Le Groupe 7 est en ligne !</h1>";
    }
}
