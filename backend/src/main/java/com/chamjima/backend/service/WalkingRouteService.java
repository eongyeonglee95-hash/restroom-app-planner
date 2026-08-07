package com.chamjima.backend.service;

import java.util.List;
import java.util.Optional;
import com.chamjima.backend.geo.GeoUtils;
import com.chamjima.backend.routing.KakaoDrivingRouteClient;
import com.chamjima.backend.routing.OsrmWalkingRouteClient;
import com.chamjima.backend.routing.TmapWalkingRouteClient;
import com.chamjima.backend.routing.TmapWalkingRouteClient.PathPoint;
import com.chamjima.backend.routing.TmapWalkingRouteClient.WalkingRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 도보 경로/시간 계산을 담당한다. 급똥 탭(화장실)과 약국 탭이 함께 사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalkingRouteService {

	private static final double AVERAGE_WALKING_SPEED_M_PER_S = 1.2;

	private final TmapWalkingRouteClient tmapWalkingRouteClient;
	private final OsrmWalkingRouteClient osrmWalkingRouteClient;
	private final KakaoDrivingRouteClient kakaoDrivingRouteClient;

	/**
	 * 도보 시간/경로를 4단계로 폴백해서 구한다:
	 * 1순위 Tmap 보행자 경로(가장 정확, 한국 도보 데이터·도보 기준 소요시간) → 2순위 OSRM
	 * 공개 데모 서버(무료, 도로를 따라가는 경로는 얻지만 이 데모 서버는 foot 프로필이
	 * driving과 동일하게 동작해 시간이 자동차 속도로 나옴 — 그래서 거리만 쓰고 시간은
	 * 도보속도로 직접 환산) → 3순위 카카오 자동차 경로(마찬가지로 거리만 쓰고 도보속도로
	 * 환산) → 4순위 직선거리 추정(셋 다 실패했을 때의 최후 수단).
	 */
	public WalkingRoute routeOrEstimate(double startLat, double startLng, double endLat, double endLng, String name) {
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
}
