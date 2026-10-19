package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.exception.BusinessException;
import fr.m2.groupe7.kiestla.service.PasswordService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller handling password operations:
 * - Authenticated password change (/change-password)
 * - Authenticated one-click password reset email (/request-password-reset)
 * - Unauthenticated forgot-password flow (/forgot-password)
 * - Password reset token confirmation (/reset-password)
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
            passwordService.changePassword(authentication.getName(), currentPassword, newPassword,
                    confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage", "votre mot de passe a été modifié");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/change-password";
    }

    /**
     * Authenticated endpoint: a logged-in student or teacher clicks "Reset password"
     * and a reset link is sent directly to their registered email address without them
     * having to enter it.
     */
    @PostMapping("/request-password-reset")
    public String requestPasswordResetAuthenticated(
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            String baseUrl = extractBaseUrl(request);
            passwordService.requestPasswordReset(authentication.getName(), baseUrl);
            redirectAttributes.addFlashAttribute("successMessage",
                    "A password reset link has been sent to your email address.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        String referer = request.getHeader("Referer");
        if (referer != null && (referer.contains("/change-password")
                || referer.contains("/student")
                || referer.contains("/teacher")
                || referer.contains("/admin")
                || referer.contains("/secretary"))) {
            return "redirect:" + referer;
        }

        return "redirect:" + homeUrlFor(authentication);
    }

    /**
     * Public page to request a password reset email if not logged in.
     */
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    /**
     * Handles public password reset requests by login or email.
     */
    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam String identifier,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            String baseUrl = extractBaseUrl(request);
            passwordService.requestPasswordReset(identifier, baseUrl);
            redirectAttributes.addFlashAttribute("successMessage",
                    "A password reset link has been sent to your email address.");
            return "redirect:/login";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    /**
     * Public page displaying the password reset form when clicking the link from the email.
     */
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("errorMessage", "Invalid password reset link.");
            return "reset-password";
        }

        try {
            passwordService.validateResetToken(token);
            model.addAttribute("token", token);
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }

        return "reset-password";
    }

    /**
     * Handles setting a new password with a valid token.
     */
    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        try {
            passwordService.resetPassword(token, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your password has been successfully reset. You can now log in.");
            return "redirect:/login";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
    }

    /**
     * Extracts the base application URL from the incoming request.
     */
    private String extractBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(serverName);
        if (("http".equalsIgnoreCase(scheme) && serverPort != 80)
                || ("https".equalsIgnoreCase(scheme) && serverPort != 443)) {
            sb.append(":").append(serverPort);
        }
        sb.append(request.getContextPath());
        return sb.toString();
    }

    /**
     * Home page of the user, used by the "Back" button.
     */
    private String homeUrlFor(Authentication authentication) {
        if (authentication == null) {
            return "/";
        }
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