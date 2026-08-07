package com.chamjima.backend.controller;

import java.util.List;
import com.chamjima.backend.dto.PharmacyResponse;
import com.chamjima.backend.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PharmacyController {

	private final PharmacyService pharmacyService;

	@GetMapping("/api/pharmacies/nearby")
	public List<PharmacyResponse> nearby(
		@RequestParam double lat,
		@RequestParam double lng,
		@RequestParam(defaultValue = "2000") double radiusMeters,
		@RequestParam(defaultValue = "30") int limit,
		@RequestParam(defaultValue = "ALL") PharmacyService.Layer layer,
		@RequestParam(defaultValue = "true") boolean openNowOnly
	) {
		return pharmacyService.findNearby(lat, lng, radiusMeters, limit, layer, openNowOnly);
	}
}
