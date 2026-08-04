package com.chamjima.backend.controller;

import java.util.List;
import com.chamjima.backend.dto.TipPlaceResponse;
import com.chamjima.backend.service.TipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TipController {

	private final TipService tipService;

	@GetMapping("/api/tips")
	public List<TipPlaceResponse> nearbyTips(
		@RequestParam double lat,
		@RequestParam double lng
	) {
		return tipService.findNearbyTips(lat, lng);
	}
}
