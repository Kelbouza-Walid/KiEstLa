package fr.m2.groupe7.kiestla.dto;

import java.util.List;

/**
 * Members management form of a module: its reference teacher, chosen among every teacher,
 * and every teacher and student flagged with whether they are currently enrolled in the module.
 * The reference teacher is not part of the teachers checkboxes: it is chosen separately.
 */
public record ModuleMembers(
		Long id,
		String name,
		Long referenceTeacherId,
		List<UserSummary> referenceTeacherChoices,
		List<Member> teachers,
		List<Member> students) {

	public record Member(UserSummary user, boolean enrolled) {
	}
}
