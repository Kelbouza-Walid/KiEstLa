package fr.m2.groupe7.kiestla.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CommentRequest
 */
public record CommentRequest(
		@NotBlank @Size(max = 2000) String comment) {
}
