package com.chamjima.backend.controller;

import java.util.List;
import com.chamjima.backend.dto.ReviewRequest;
import com.chamjima.backend.dto.ReviewResponse;
import com.chamjima.backend.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;

	@GetMapping("/api/restrooms/{restroomId}/reviews")
	public List<ReviewResponse> list(@PathVariable Long restroomId) {
		return reviewService.findByRestroomId(restroomId);
	}

	@PostMapping("/api/restrooms/{restroomId}/reviews")
	@ResponseStatus(HttpStatus.CREATED)
	public ReviewResponse create(@PathVariable Long restroomId, @RequestBody ReviewRequest request) {
		return reviewService.create(restroomId, request);
	}
}
