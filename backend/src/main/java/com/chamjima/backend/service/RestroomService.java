package com.chamjima.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import com.chamjima.backend.domain.Restroom;
import com.chamjima.backend.dto.NearbyRestroomResponse;
import com.chamjima.backend.dto.TipPlaceResponse;
import com.chamjima.backend.dto.UrgentPlaceResponse;
import com.chamjima.backend.geo.GeoUtils;
import com.chamjima.backend.repository.RestroomRepository;
import com.chamjima.backend.routing.KakaoDrivingRouteClient;
import com.chamjima.backend.routing.OsrmWalkingRouteClient;
import com.chamjima.backend.routing.TmapWalkingRouteClient;
import com.chamjima.backend.routing.TmapWalkingRouteClient.PathPoint;
import com.chamjima.backend.routing.TmapWalkingRouteClient.WalkingRoute;
import com.chamjima.backend.service.ReviewService.RatingSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestroomService {

	private static final double URGENT_CANDIDATE_RADIUS_METERS = 1500;
	private static final int URGENT_CANDIDATE_COUNT = 8;
	private static final int URGENT_TIP_CANDIDATE_COUNT = 4;
	private static final double AVERAGE_WALKING_SPEED_M_PER_S = 1.2;

	private final RestroomRepository restroomRepository;
	private final TmapWalkingRouteClient tmapWalkingRouteClient;
	private final OsrmWalkingRouteClient osrmWalkingRouteClient;
	private final KakaoDrivingRouteClient kakaoDrivingRouteClient;
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

	/**
	 * 도보 시간/경로를 4단계로 폴백해서 구한다:
	 * 1순위 Tmap 보행자 경로(가장 정확, 한국 도보 데이터·도보 기준 소요시간) → 2순위 OSRM
	 * 공개 데모 서버(무료, 도로를 따라가는 경로는 얻지만 이 데모 서버는 foot 프로필이
	 * driving과 동일하게 동작해 시간이 자동차 속도로 나옴 — 그래서 거리만 쓰고 시간은
	 * 도보속도로 직접 환산) → 3순위 카카오 자동차 경로(마찬가지로 거리만 쓰고 도보속도로
	 * 환산) → 4순위 직선거리 추정(셋 다 실패했을 때의 최후 수단).
	 */
	private WalkingRoute walkingRouteOrEstimate(double startLat, double startLng, double endLat, double endLng, String name) {
		try {
			Optional<WalkingRoute> tmapRoute = tmapWalkingRouteClient.route(startLat, startLng, endLat, endLng);
			if (tmapRoute.isPresent()) {
				return tmapRoute.get();
			}
		} catch (Exception e) {
			log.warn("Tmap 도보 경로 호출 실패: {} - {}", name, e.getMessage());
		}

		try {
			Optional<WalkingRoute> osrmRoute = osrmWalkingRouteClient.route(startLat, startLng, endLat, endLng);
			if (osrmRoute.isPresent()) {
				WalkingRoute raw = osrmRoute.get();
				int timeSeconds = (int) Math.round(raw.totalDistanceMeters() / AVERAGE_WALKING_SPEED_M_PER_S);
				log.info("Tmap 실패, OSRM 도로 경로 + 도보속도 환산으로 대체: {}", name);
				return new WalkingRoute(timeSeconds, raw.totalDistanceMeters(), raw.path());
			}
		} catch (Exception e) {
			log.warn("OSRM 도보 경로 호출 실패: {} - {}", name, e.getMessage());
		}

		try {
			Optional<KakaoDrivingRouteClient.RoadRoute> roadRoute =
				kakaoDrivingRouteClient.route(startLat, startLng, endLat, endLng);
			if (roadRoute.isPresent()) {
				KakaoDrivingRouteClient.RoadRoute road = roadRoute.get();
				int timeSeconds = (int) Math.round(road.distanceMeters() / AVERAGE_WALKING_SPEED_M_PER_S);
				log.info("Tmap 실패, 카카오 도로 경로로 대체: {}", name);
				return new WalkingRoute(timeSeconds, road.distanceMeters(), road.path());
			}
		} catch (Exception e) {
			log.warn("카카오 도로 경로 호출도 실패: {} - {}", name, e.getMessage());
		}

		log.warn("도보/도로 경로 모두 실패, 직선거리로 추정: {}", name);
		return estimate(startLat, startLng, endLat, endLng);
	}

	private WalkingRoute estimate(double startLat, double startLng, double endLat, double endLng) {
		double distance = GeoUtils.haversineMeters(startLat, startLng, endLat, endLng);
		int timeSeconds = (int) Math.round(distance / AVERAGE_WALKING_SPEED_M_PER_S);
		List<PathPoint> path = List.of(new PathPoint(startLat, startLng), new PathPoint(endLat, endLng));
		return new WalkingRoute(timeSeconds, (int) Math.round(distance), path);
	}

	private UrgentPlaceResponse toRestroomUrgentPlace(NearbyRestroomResponse candidate, double lat, double lng) {
		WalkingRoute route = walkingRouteOrEstimate(lat, lng, candidate.latitude(), candidate.longitude(), candidate.name());
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
			candidate.averageRating(),
			candidate.reviewCount(),
			UrgencyScoreCalculator.calculate(route.totalTimeSeconds(), candidate.averageRating(), candidate.reviewCount()),
			route.totalTimeSeconds(),
			route.totalDistanceMeters(),
			route.path()
		);
	}

	private UrgentPlaceResponse toTipUrgentPlace(TipPlaceResponse candidate, double lat, double lng) {
		WalkingRoute route = walkingRouteOrEstimate(lat, lng, candidate.latitude(), candidate.longitude(), candidate.name());
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
			null,
			0,
			null,
			route.totalTimeSeconds(),
			route.totalDistanceMeters(),
			route.path()
		);
	}
}
