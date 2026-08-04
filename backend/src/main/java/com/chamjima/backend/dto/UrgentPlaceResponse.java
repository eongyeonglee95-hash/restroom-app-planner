package com.chamjima.backend.dto;

import java.util.List;
import com.chamjima.backend.routing.TmapWalkingRouteClient.PathPoint;

public record UrgentPlaceResponse(
	String id,
	String type,
	String name,
	String category,
	String address,
	double latitude,
	double longitude,
	String openHours,
	boolean hasDiaperTable,
	String tip,
	Double averageRating,
	int reviewCount,
	Integer urgencyScore,
	int walkingTimeSeconds,
	int walkingDistanceMeters,
	List<PathPoint> path
) {
	public static final String TYPE_RESTROOM = "RESTROOM";
	public static final String TYPE_TIP = "TIP";
}
