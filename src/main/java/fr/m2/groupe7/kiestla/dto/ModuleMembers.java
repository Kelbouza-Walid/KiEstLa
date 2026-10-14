package fr.m2.groupe7.kiestla.dto;

import java.util.List;

/**
 * Members management form of a module: every teacher and student,
 * each flagged with whether they are currently enrolled in the module.
 * The reference teacher is not part of the teachers list: it is managed separately.
 */
public record ModuleMembers(
		Long id,
		String name,
		String referenceTeacherName,
		List<Member> teachers,
		List<Member> students) {

	public record Member(UserSummary user, boolean enrolled) {
	}
}
