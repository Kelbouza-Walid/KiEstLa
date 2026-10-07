package fr.m2.groupe7.kiestla.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JustifyAbsenceRequest
 */
public record JustifyAbsenceRequest(
		@NotBlank @Size(max = 2000) String justification) {
}
