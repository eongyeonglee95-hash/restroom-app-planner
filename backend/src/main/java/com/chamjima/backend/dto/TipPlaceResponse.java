package com.chamjima.backend.dto;

public record TipPlaceResponse(
	String name,
	String category,
	String address,
	double latitude,
	double longitude,
	String tip
) {
}
