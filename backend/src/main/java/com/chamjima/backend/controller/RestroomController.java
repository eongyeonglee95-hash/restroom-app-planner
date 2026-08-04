package com.chamjima.backend.controller;

import java.util.List;
import com.chamjima.backend.dto.NearbyRestroomResponse;
import com.chamjima.backend.dto.UrgentPlaceResponse;
import com.chamjima.backend.service.RestroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RestroomController {

	private final RestroomService restroomService;

	@GetMapping("/api/restrooms/nearby")
	public List<NearbyRestroomResponse> nearby(
		@RequestParam double lat,
		@RequestParam double lng,
		@RequestParam(defaultValue = "2000") double radiusMeters,
		@RequestParam(defaultValue = "30") int limit
	) {
		return restroomService.findNearby(lat, lng, radiusMeters, limit);
	}

	@GetMapping("/api/restrooms/urgent")
	public List<UrgentPlaceResponse> urgent(
		@RequestParam double lat,
		@RequestParam double lng,
		@RequestParam(defaultValue = "true") boolean includeTips
	) {
		return restroomService.findUrgent(lat, lng, includeTips);
	}
}
