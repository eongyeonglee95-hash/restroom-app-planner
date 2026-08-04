package com.chamjima.backend.dto;

public record NearbyRestroomResponse(
	Long id,
	String name,
	String category,
	String address,
	double latitude,
	double longitude,
	String openHours,
	boolean hasDiaperTable,
	double distanceMeters,
	Double averageRating,
	int reviewCount
) {
}
