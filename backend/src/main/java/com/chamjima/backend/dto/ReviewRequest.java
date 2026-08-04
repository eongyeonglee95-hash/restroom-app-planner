package com.chamjima.backend.dto;

import com.chamjima.backend.domain.Review;

public record ReviewRequest(
	Review.Mood mood,
	boolean hasTissue,
	boolean hasBidet,
	boolean noLine,
	boolean isFree,
	boolean isClean,
	boolean noPasscode,
	String comment
) {
}
