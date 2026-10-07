package fr.m2.groupe7.kiestla.dto;

import fr.m2.groupe7.kiestla.entity.User;

/**
 * Minimal user shape for lists and selects.
 * Does not contain the password or the role.
 */
public record UserSummary(
		Long id,
		String firstName,
		String lastName,
		String login) {

	public static UserSummary from(User u) {
		return new UserSummary(u.getId(), u.getFirstName(), u.getLastName(), u.getLogin());
	}
}
