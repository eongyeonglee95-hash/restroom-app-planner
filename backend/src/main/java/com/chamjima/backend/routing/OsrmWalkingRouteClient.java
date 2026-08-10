package com.chamjima.backend.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.chamjima.backend.routing.TmapWalkingRouteClient.PathPoint;
import com.chamjima.backend.routing.TmapWalkingRouteClient.WalkingRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OSRM(오픈소스 라우팅 엔진)의 foot(도보) 프로필로 실제 보행 경로를 계산한다.
 *
 * <p><b>반드시 foot 프로필로 전처리한 자체 OSRM 인스턴스를 가리켜야 한다.</b> 공개 데모
 * 서버(router.project-osrm.org)는 두 가지 이유로 쓰지 않는다: (1) 개발·데모용이라 서비스
 * 트래픽을 보내면 안 되고, (2) 그 서버의 foot 프로필이 실제로는 자동차처럼 동작해서
 * 소요시간이 시속 34~54km로 나온다(실측). 자체 인스턴스는 시속 5km로 일관되게 나온다.
 *
 * <p>거리도 다르다 — 실측에서 같은 구간이 차도 기준보다 16% 길게(보행 불가 구간 우회),
 * 다른 구간은 짧게(보행자 전용 지름길) 나왔다. 그래서 duration을 그대로 신뢰한다.
 *
 * <p>준비 방법은 README의 "도보 경로 서버(OSRM)" 항목 참고. 서버가 없으면 이 클라이언트는
 * 조용히 실패하고 상위의 폴백(Tmap → 카카오 → 직선거리)으로 넘어간다.
 */
@Slf4j
@Component
public class OsrmWalkingRouteClient {

	private final RestClient restClient;

	public OsrmWalkingRouteClient(@Value("${app.routing.osrm.base-url:http://localhost:5001}") String baseUrl) {
		this.restClient = RestClient.builder()
			.baseUrl(baseUrl)
			.defaultHeader("User-Agent", "chamjima-app")
			.defaultHeader("Accept-Encoding", "identity")
			.build();
	}

	@SuppressWarnings("unchecked")
	public Optional<WalkingRoute> route(double startLat, double startLng, double endLat, double endLng) {
		// 세미콜론(;)이 좌표 구분자인데, URI 템플릿 변수로 넘기면 %3B로 인코딩돼서
		// OSRM이 요청을 못 알아듣는다 - 경로 문자열을 직접 조립해서 그대로 넘긴다.
		String coords = startLng + "," + startLat + ";" + endLng + "," + endLat;
		String path = "/route/v1/foot/" + coords;

		Map<String, Object> response;
		try {
			response = restClient.get()
				.uri(uriBuilder -> uriBuilder
					.path(path)
					.queryParam("overview", "full")
					.queryParam("geometries", "geojson")
					.build())
				.retrieve()
				.body(Map.class);
		} catch (Exception e) {
			log.warn("OSRM 요청 실패", e);
			return Optional.empty();
		}

		if (response == null || !"Ok".equals(response.get("code"))) {
			log.warn("OSRM 응답 비정상: {}", response == null ? "null" : response.get("code"));
			return Optional.empty();
		}

		List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
		if (routes == null || routes.isEmpty()) {
			return Optional.empty();
		}

		Map<String, Object> route = routes.get(0);
		List<Map<String, Object>> legs = (List<Map<String, Object>>) route.get("legs");
		if (legs == null || legs.isEmpty()) {
			return Optional.empty();
		}

		Map<String, Object> leg = legs.get(0);
		int durationSeconds = (int) Math.round(((Number) leg.get("duration")).doubleValue());
		int distanceMeters = (int) Math.round(((Number) leg.get("distance")).doubleValue());

		Map<String, Object> geometry = (Map<String, Object>) route.get("geometry");
		List<List<Number>> coordinates = (List<List<Number>>) geometry.get("coordinates");
		List<PathPoint> path2 = new ArrayList<>();
		for (List<Number> coordinate : coordinates) {
			double lng = coordinate.get(0).doubleValue();
			double lat = coordinate.get(1).doubleValue();
			path2.add(new PathPoint(lat, lng));
		}

		return Optional.of(new WalkingRoute(durationSeconds, distanceMeters, path2));
	}
}
