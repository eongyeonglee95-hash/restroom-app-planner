package com.chamjima.backend.service;

import java.util.List;
import java.util.Optional;
import com.chamjima.backend.geo.GeoUtils;
import com.chamjima.backend.routing.KakaoDrivingRouteClient;
import com.chamjima.backend.routing.OsrmWalkingRouteClient;
import com.chamjima.backend.routing.TmapWalkingRouteClient;
import com.chamjima.backend.routing.TmapWalkingRouteClient.PathPoint;
import com.chamjima.backend.routing.TmapWalkingRouteClient.WalkingRoute;
import io.micrometer.core.instrument.MeterRegistry;
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

	/**
	 * 어느 단계에서 경로가 나왔는지 센다. 자체 OSRM 비중이 높아야 정상이고, tmap/kakao/estimate가
	 * 늘고 있으면 OSRM이 죽었거나 서울 밖 요청이 들어오고 있다는 뜻이다. 서버 사양을 바꿀지
	 * 판단하는 근거로 쓴다. 조회: GET {management-port}/actuator/metrics/chamjima.walking.route
	 */
	private static final String ROUTE_PROVIDER_METRIC = "chamjima.walking.route";

	private final TmapWalkingRouteClient tmapWalkingRouteClient;
	private final OsrmWalkingRouteClient osrmWalkingRouteClient;
	private final KakaoDrivingRouteClient kakaoDrivingRouteClient;
	private final MeterRegistry meterRegistry;

	/**
	 * 도보 시간/경로를 4단계로 폴백해서 구한다:
	 * 1순위 자체 OSRM(foot 프로필 — 보행자 전용길·지름길까지 반영된 거리와 시속 5km 기준
	 * 소요시간을 그대로 신뢰. 할당량이 없어 후보 전부에 쓸 수 있다) → 2순위 Tmap 보행자
	 * 경로(정확하지만 무료 1,000건/일이라 자체 OSRM이 죽었을 때만) → 3순위 카카오 자동차
	 * 경로(거리만 쓰고 도보속도로 환산) → 4순위 직선거리 추정(전부 실패했을 때의 최후 수단).
	 *
	 * <p>순서가 Tmap 우선에서 OSRM 우선으로 바뀐 이유는 자체 OSRM을 띄우면서 정확도 문제가
	 * 사라졌기 때문이다(2026-08-10). 공개 데모 서버를 쓰던 시절에는 시간이 자동차 속도로
	 * 나와서 거리만 뽑아 쓰는 처리가 필요했지만, foot 프로필로 직접 전처리한 지금은 불필요하다.
	 */
	public WalkingRoute routeOrEstimate(double startLat, double startLng, double endLat, double endLng, String name) {
		try {
			Optional<WalkingRoute> osrmRoute = osrmWalkingRouteClient.route(startLat, startLng, endLat, endLng);
			if (osrmRoute.isPresent()) {
				countProvider("osrm");
				return osrmRoute.get();
			}
		} catch (Exception e) {
			log.warn("자체 OSRM 도보 경로 호출 실패: {} - {}", name, e.getMessage());
		}

		try {
			Optional<WalkingRoute> tmapRoute = tmapWalkingRouteClient.route(startLat, startLng, endLat, endLng);
			if (tmapRoute.isPresent()) {
				log.info("OSRM 실패, Tmap 보행자 경로로 대체: {}", name);
				countProvider("tmap");
				return tmapRoute.get();
			}
		} catch (Exception e) {
			log.warn("Tmap 도보 경로 호출 실패: {} - {}", name, e.getMessage());
		}

		try {
			Optional<KakaoDrivingRouteClient.RoadRoute> roadRoute =
				kakaoDrivingRouteClient.route(startLat, startLng, endLat, endLng);
			if (roadRoute.isPresent()) {
				KakaoDrivingRouteClient.RoadRoute road = roadRoute.get();
				int timeSeconds = (int) Math.round(road.distanceMeters() / AVERAGE_WALKING_SPEED_M_PER_S);
				log.info("OSRM·Tmap 실패, 카카오 도로 경로로 대체: {}", name);
				countProvider("kakao");
				return new WalkingRoute(timeSeconds, road.distanceMeters(), road.path());
			}
		} catch (Exception e) {
			log.warn("카카오 도로 경로 호출도 실패: {} - {}", name, e.getMessage());
		}

		log.warn("도보/도로 경로 모두 실패, 직선거리로 추정: {}", name);
		countProvider("estimate");
		return estimate(startLat, startLng, endLat, endLng);
	}

	private void countProvider(String provider) {
		meterRegistry.counter(ROUTE_PROVIDER_METRIC, "provider", provider).increment();
	}

	private WalkingRoute estimate(double startLat, double startLng, double endLat, double endLng) {
		double distance = GeoUtils.haversineMeters(startLat, startLng, endLat, endLng);
		int timeSeconds = (int) Math.round(distance / AVERAGE_WALKING_SPEED_M_PER_S);
		List<PathPoint> path = List.of(new PathPoint(startLat, startLng), new PathPoint(endLat, endLng));
		return new WalkingRoute(timeSeconds, (int) Math.round(distance), path);
	}
}
