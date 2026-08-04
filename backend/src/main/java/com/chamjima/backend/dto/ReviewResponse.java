package com.chamjima.backend.dto;

import java.time.LocalDateTime;
import com.chamjima.backend.domain.Review;

public record ReviewResponse(
	Long id,
	Review.Mood mood,
	boolean hasTissue,
	boolean hasBidet,
	boolean noLine,
	boolean isFree,
	boolean isClean,
	boolean noPasscode,
	String comment,
	LocalDateTime createdAt
) {
	public static ReviewResponse from(Review review) {
		return new ReviewResponse(
			review.getId(),
			review.getMood(),
			review.isHasTissue(),
			review.isHasBidet(),
			review.isNoLine(),
			review.isFree(),
			review.isClean(),
			review.isNoPasscode(),
			review.getComment(),
			review.getCreatedAt()
		);
	}
}
