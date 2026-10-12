package fr.m2.groupe7.kiestla.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AddCommentRequest
 */
public record AddCommentRequest(
		@NotBlank @Size(max = 2000) String comment) {
}
