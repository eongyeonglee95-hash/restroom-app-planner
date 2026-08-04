package com.chamjima.backend.service;

public final class UrgencyScoreCalculator {

	private static final double NEUTRAL_REVIEW_SCORE = 60;

	private UrgencyScoreCalculator() {
	}

	public static int calculate(int walkingTimeSeconds, Double averageRating, int reviewCount) {
		double distanceScore = clamp(100 - walkingTimeSeconds / 6.0, 0, 100);
		double reviewScore = reviewCount > 0 && averageRating != null
			? clamp((averageRating / 5.0) * 100, 0, 100)
			: NEUTRAL_REVIEW_SCORE;

		double score = 0.6 * distanceScore + 0.4 * reviewScore;
		return (int) Math.round(clamp(score, 0, 100));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
