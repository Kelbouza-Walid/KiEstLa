package fr.m2.groupe7.kiestla.dto;

import java.time.LocalDate;

import fr.m2.groupe7.kiestla.entity.Absence;
import fr.m2.groupe7.kiestla.entity.AbsenceStatus;

/**
 * Absence shape for the absence tables (secretary, module and teacher pages).
 */
public record AbsenceSummary(
		Long id,
		String studentName,
		LocalDate date,
		AbsenceStatus status,
		String justification) {

	public static AbsenceSummary from(Absence a) {
		return new AbsenceSummary(a.getId(),
				a.getStudent().getFirstName() + " " + a.getStudent().getLastName(),
				a.getDate(), a.getStatus(), a.getJustification());
	}
}
