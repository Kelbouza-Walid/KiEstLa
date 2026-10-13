package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.service.PasswordService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Change-password page, available to every authenticated user
 * (SecurityConfig: anyRequest().authenticated()).
 */
@Controller
public class PasswordController {

    private final PasswordService passwordService;

    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @GetMapping("/change-password")
    public String changePasswordPage(Authentication authentication, Model model) {
        model.addAttribute("homeUrl", homeUrlFor(authentication));
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            // The login comes from the session, never from the request:
            // a user can only change his own password.
            passwordService.changePassword(authentication.getName(), currentPassword, newPassword,
                    confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage", "votre mot de passe a été modifié");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/change-password";
    }

    /**
     * Home page of the user, used by the "Back" button.
     * We loop because the authorities also contain technical entries (FACTOR_PASSWORD).
     */
    private String homeUrlFor(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_TEACHER".equals(role)) {
                return "/teacher";
            } else if ("ROLE_SECRETARY".equals(role)) {
                return "/secretary";
            } else if ("ROLE_ADMIN".equals(role)) {
                return "/admin";
            } else if ("ROLE_STUDENT".equals(role)) {
                return "/student";
            }
        }
        return "/";
    }
}