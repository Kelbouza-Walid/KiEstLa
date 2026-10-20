package fr.m2.groupe7.kiestla.dto;

import fr.m2.groupe7.kiestla.entity.Role;

import java.util.List;

/**
 * Full view of a user for the administrator: personal information,
 * enrolled modules, modules the user is the reference teacher of, and number of absences.
 */
public record AdminUserDetails(
        Long id,
        String firstName,
        String lastName,
        String login,
        String email,
        Role role,
        List<ModuleSummary> enrolledModules,
        List<ModuleSummary> referencedModules,
        long absenceCount) {

    public AdminUserDetails(Long id, String firstName, String lastName, String login, Role role,
            List<ModuleSummary> enrolledModules, List<ModuleSummary> referencedModules, long absenceCount) {
        this(id, firstName, lastName, login, null, role, enrolledModules, referencedModules, absenceCount);
    }
}
