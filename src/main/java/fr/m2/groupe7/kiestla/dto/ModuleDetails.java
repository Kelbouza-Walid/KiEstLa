package fr.m2.groupe7.kiestla.dto;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.Role;
import fr.m2.groupe7.kiestla.entity.User;

/**
 * Detailled module shape
 * Contains the module header plus info on enrolled participants and teachers.
 */
public record ModuleDetails(
		Long id,
		String name,
		String referenceTeacherName,
		List<UserSummary> students,
		List<UserSummary> teachers) {

	private static final Comparator<User> BY_NAME = Comparator.comparing(User::getLastName)
			.thenComparing(User::getFirstName);

	public static ModuleDetails from(Module m) {
		User reference = m.getReferenceTeacher();
		return new ModuleDetails(
				m.getId(),
				m.getName(),
				reference != null ? reference.getFirstName() + " " + reference.getLastName() : null,
				m.getEnrolledStudents().stream()
						.sorted(BY_NAME)
						.map(UserSummary::from)
						.toList(),
				m.getAllTeachers().stream()
						.sorted(BY_NAME)
						.map(UserSummary::from)
						.toList());
	}
}
