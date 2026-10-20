package fr.m2.groupe7.kiestla.dto;

import fr.m2.groupe7.kiestla.entity.Module;
import fr.m2.groupe7.kiestla.entity.User;

/**
 * Light module shape for lists
 */
public record ModuleSummary(
		Long id,
		String name,
		String referenceTeacherLastName) {

	public static ModuleSummary from(Module m) {
		User reference = m.getReferenceTeacher();
		return new ModuleSummary(m.getId(), m.getName(),
				reference != null ? reference.getLastName() : null);
	}
}
