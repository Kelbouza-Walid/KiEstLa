package fr.m2.groupe7.kiestla.dto;

/**
 * Result of a user creation.
 * The generated password is only kept in clear here, so it can be shown once to the administrator.
 */
public record CreatedUser(
        AdminUserSummary user,
        String generatedPassword) {
}
