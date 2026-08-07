package com.chamjima.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import com.chamjima.backend.domain.Restroom;
import com.chamjima.backend.dto.NearbyRestroomResponse;
import com.chamjima.backend.dto.TipPlaceResponse;
import com.chamjima.backend.dto.UrgentPlaceResponse;
import com.chamjima.backend.geo.GeoUtils;
import com.chamjima.backend.repository.RestroomRepository;
import com.chamjima.backend.routing.TmapWalkingRouteClient.WalkingRoute;
import com.chamjima.backend.service.ReviewService.RatingSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestroomService {

	private static final double URGENT_CANDIDATE_RADIUS_METERS = 1500;
	private static final int URGENT_CANDIDATE_COUNT = 8;
	private static final int URGENT_TIP_CANDIDATE_COUNT = 4;

	private final RestroomRepository restroomRepository;
	private final WalkingRouteService walkingRouteService;
	private final TipService tipService;
	private final ReviewService reviewService;

	public List<NearbyRestroomResponse> findNearby(double lat, double lng, double radiusMeters, int limit) {
		List<Restroom> candidates = restroomRepository.findByStatusAndLatitudeIsNotNull(Restroom.Status.ACTIVE);

		List<NearbyRestroomResponse> withoutRatings = candidates.stream()
			.map(restroom -> {
				double distance = GeoUtils.haversineMeters(lat, lng, restroom.getLatitude(), restroom.getLongitude());
				return new NearbyRestroomResponse(
					restroom.getId(),
					restroom.getName(),
					restroom.getCategory(),
					restroom.getAddress(),
					restroom.getLatitude(),
					restroom.getLongitude(),
					restroom.getOpenHours(),
					restroom.isHasDiaperTable(),
					distance,
					null,
					0
				);
			})
			.filter(dto -> dto.distanceMeters() <= radiusMeters)
			.sorted(Comparator.comparingDouble(NearbyRestroomResponse::distanceMeters))
			.limit(limit)
			.toList();

		Map<Long, RatingSummary> ratings =
			reviewService.summarizeByRestroomIds(withoutRatings.stream().map(NearbyRestroomResponse::id).toList());

		return withoutRatings.stream()
			.map(dto -> {
				RatingSummary rating = ratings.get(dto.id());
				if (rating == null) {
					return dto;
				}
				return new NearbyRestroomResponse(
					dto.id(), dto.name(), dto.category(), dto.address(), dto.latitude(), dto.longitude(),
					dto.openHours(), dto.hasDiaperTable(), dto.distanceMeters(),
					rating.averageRating(), rating.reviewCount()
				);
			})
			.toList();
	}

	public List<UrgentPlaceResponse> findUrgent(double lat, double lng, boolean includeTips) {
		List<NearbyRestroomResponse> restroomCandidates =
			findNearby(lat, lng, URGENT_CANDIDATE_RADIUS_METERS, URGENT_CANDIDATE_COUNT);

		Stream<UrgentPlaceResponse> restroomResults = restroomCandidates.stream()
			.map(candidate -> toRestroomUrgentPlace(candidate, lat, lng))
			.filter(Objects::nonNull);

		Stream<UrgentPlaceResponse> tipResults = Stream.empty();
		if (includeTips) {
			List<TipPlaceResponse> tipCandidates = tipService.findNearbyTips(lat, lng).stream()
				.sorted(Comparator.comparingDouble(tip ->
					GeoUtils.haversineMeters(lat, lng, tip.latitude(), tip.longitude())))
				.limit(URGENT_TIP_CANDIDATE_COUNT)
				.toList();

			tipResults = tipCandidates.stream()
				.map(candidate -> toTipUrgentPlace(candidate, lat, lng))
				.filter(Objects::nonNull);
		}

		return Stream.concat(restroomResults, tipResults)
			.sorted(Comparator.comparingInt(UrgentPlaceResponse::walkingTimeSeconds))
			.toList();
	}

	private UrgentPlaceResponse toRestroomUrgentPlace(NearbyRestroomResponse candidate, double lat, double lng) {
		WalkingRoute route = walkingRouteService.routeOrEstimate(
			lat, lng, candidate.latitude(), candidate.longitude(), candidate.name());
		return new UrgentPlaceResponse(
			String.valueOf(candidate.id()),
			UrgentPlaceResponse.TYPE_RESTROOM,
			candidate.name(),
			candidate.category(),
			candidate.address(),
			candidate.latitude(),
			candidate.longitude(),
			candidate.openHours(),
			candidate.hasDiaperTable(),
			null,
			null,
			candidate.averageRating(),
			candidate.reviewCount(),
			UrgencyScoreCalculator.calculate(route.totalTimeSeconds(), candidate.averageRating(), candidate.reviewCount()),
			route.totalTimeSeconds(),
			route.totalDistanceMeters(),
			route.path()
		);
	}

	private UrgentPlaceResponse toTipUrgentPlace(TipPlaceResponse candidate, double lat, double lng) {
		WalkingRoute route = walkingRouteService.routeOrEstimate(
			lat, lng, candidate.latitude(), candidate.longitude(), candidate.name());
		return new UrgentPlaceResponse(
			candidate.name() + "@" + candidate.latitude() + "," + candidate.longitude(),
			UrgentPlaceResponse.TYPE_TIP,
			candidate.name(),
			candidate.category(),
			candidate.address(),
			candidate.latitude(),
			candidate.longitude(),
			null,
			false,
			candidate.tip(),
			candidate.phone(),
			null,
			0,
			null,
			route.totalTimeSeconds(),
			route.totalDistanceMeters(),
			route.path()
		);
	}
}
