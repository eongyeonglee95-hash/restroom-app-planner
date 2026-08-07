package com.chamjima.backend.dto;

import java.util.List;
import com.chamjima.backend.routing.TmapWalkingRouteClient.PathPoint;

public record PharmacyResponse(
	String id,
	String type,
	String name,
	String address,
	String phone,
	double latitude,
	double longitude,
	double distanceMeters,
	boolean openNow,
	String todayHours,
	int walkingTimeSeconds,
	int walkingDistanceMeters,
	List<PathPoint> path
) {
	public static final String TYPE_GENERAL = "GENERAL";
	public static final String TYPE_NIGHT = "NIGHT";
}
