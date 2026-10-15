package fr.m2.groupe7.kiestla.controller;

import fr.m2.groupe7.kiestla.dto.AdminUserSummary;
import fr.m2.groupe7.kiestla.dto.CreateUserRequest;
import fr.m2.groupe7.kiestla.dto.CreatedUser;
import fr.m2.groupe7.kiestla.dto.UpdateUserRequest;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.service.UserService;
import jakarta.validation.Valid;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

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
     * Displays the admin dashboard, with the users matching the optional search criteria.
     *
     * @param q    text to look for in the users' names and logins
     * @param role role to filter the users on
     * @return the name of the Thymeleaf template to render
     */
    @GetMapping
    public String index(@RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            Model model) {
        model.addAttribute("users", userService.searchUsers(q, role));
        model.addAttribute("roles", Role.values());
        model.addAttribute("searchQuery", q);
        model.addAttribute("searchRole", role);
        return "admin";
    }

    /**
     * Displays the details of a user.
     *
     * @return the name of the Thymeleaf template to render
     */
    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        model.addAttribute("userDetails", userService.getUserDetails(id));
        return "admin-user-details";
    }

    /**
     * Creates a new user.
     *
     * @return a redirection to the admin dashboard
     */
    @PostMapping("/users")
    public String createUser(
            @Valid @ModelAttribute CreateUserRequest createUserRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Invalid user information.");
            return "redirect:/admin";
        }
        CreatedUser created = userService.createUser(createUserRequest);
        redirectAttributes.addFlashAttribute("successMessage",
                "User " + created.user().login() + " has been created.");
        redirectAttributes.addFlashAttribute("generatedPassword", created.generatedPassword());
        return "redirect:/admin";
    }

    /**
     * Displays the form to update a user.
     *
     * @return the name of the Thymeleaf template to render
     */
    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        model.addAttribute("editedUser", userService.getUser(id));
        model.addAttribute("roles", Role.values());
        return "admin-user-edit";
    }

    /**
     * Updates an existing user.
     *
     * @return a redirection to the admin dashboard, or back to the form if the information is invalid
     */
    @PostMapping("/users/{id}")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateUserRequest updateUserRequest,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Invalid user information.");
            return "redirect:/admin/users/" + id + "/edit";
        }
        AdminUserSummary updated = userService.updateUser(id, updateUserRequest, principal.getName());
        redirectAttributes.addFlashAttribute("successMessage",
                "User " + updated.login() + " has been updated.");
        return "redirect:/admin";
    }
}
