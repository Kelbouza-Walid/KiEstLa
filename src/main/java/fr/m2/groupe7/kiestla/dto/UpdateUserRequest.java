package fr.m2.groupe7.kiestla.dto;

import fr.m2.groupe7.kiestla.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form submitted by an administrator to update an existing user.
 * The password is not part of the request: it cannot be changed from this form.
 */
public record UpdateUserRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 50) String login,
        @NotNull Role role) {
}
