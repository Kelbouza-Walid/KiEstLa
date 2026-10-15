package fr.m2.groupe7.kiestla.dto;

import fr.m2.groupe7.kiestla.entity.Comment;

/**
 * CommentSummary
 */
public record CommentSummary(
		Long id,
		String userLogin,
		String userFirstName,
		String userLastName,
		String comment) {

	public static CommentSummary from(Comment c) {
		return new CommentSummary(c.getId(), c.getCommenter().getLogin(), c.getCommenter().getFirstName(),
				c.getCommenter().getLastName(), c.getComment());
	}
}
