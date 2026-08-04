package com.chamjima.backend.service;

import java.util.List;
import java.util.Map;
import com.chamjima.backend.domain.Review;
import com.chamjima.backend.dto.ReviewRequest;
import com.chamjima.backend.dto.ReviewResponse;
import com.chamjima.backend.repository.ReviewRepository;
import com.chamjima.backend.repository.RestroomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final RestroomRepository restroomRepository;

	public static double moodScore(Review.Mood mood) {
		return switch (mood) {
			case GOOD -> 5.0;
			case NORMAL -> 3.0;
			case BAD -> 1.0;
		};
	}

	public List<ReviewResponse> findByRestroomId(Long restroomId) {
		return reviewRepository.findByRestroomIdOrderByCreatedAtDesc(restroomId).stream()
			.map(ReviewResponse::from)
			.toList();
	}

	public ReviewResponse create(Long restroomId, ReviewRequest request) {
		if (!restroomRepository.existsById(restroomId)) {
			throw new IllegalArgumentException("존재하지 않는 화장실입니다: " + restroomId);
		}

		Review review = new Review();
		review.setRestroomId(restroomId);
		review.setMood(request.mood());
		review.setHasTissue(request.hasTissue());
		review.setHasBidet(request.hasBidet());
		review.setNoLine(request.noLine());
		review.setFree(request.isFree());
		review.setClean(request.isClean());
		review.setNoPasscode(request.noPasscode());
		review.setComment(normalizeComment(request.comment()));

		return ReviewResponse.from(reviewRepository.save(review));
	}

	private String normalizeComment(String comment) {
		if (comment == null) {
			return null;
		}
		String trimmed = comment.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
	}

	public Map<Long, RatingSummary> summarizeByRestroomIds(List<Long> restroomIds) {
		Map<Long, List<Review>> byRestroom = reviewRepository.findByRestroomIdIn(restroomIds).stream()
			.collect(java.util.stream.Collectors.groupingBy(Review::getRestroomId));

		return byRestroom.entrySet().stream()
			.collect(java.util.stream.Collectors.toMap(
				Map.Entry::getKey,
				entry -> {
					List<Review> reviews = entry.getValue();
					double average = reviews.stream().mapToDouble(r -> moodScore(r.getMood())).average().orElse(0);
					return new RatingSummary(average, reviews.size());
				}
			));
	}

	public record RatingSummary(double averageRating, int reviewCount) {
	}
}
