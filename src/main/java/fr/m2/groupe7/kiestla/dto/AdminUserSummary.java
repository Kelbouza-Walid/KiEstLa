package fr.m2.groupe7.kiestla.dto;

import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;

public record AdminUserSummary(
        Long id,
        String firstName,
        String lastName,
        String login,
        Role role) {

    public static AdminUserSummary from(User user) {
        return new AdminUserSummary(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getLogin(),
                user.getRole());
    }
}
