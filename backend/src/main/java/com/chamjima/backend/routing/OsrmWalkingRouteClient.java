package com.chamjima.backend.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.chamjima.backend.routing.TmapWalkingRouteClient.PathPoint;
import com.chamjima.backend.routing.TmapWalkingRouteClient.WalkingRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OSRM(오픈소스 라우팅 엔진) 공개 데모 서버의 foot(도보) 프로필. 인증키 없이 무료로
 * 실제 도보 경로를 계산해준다. 다만 커뮤니티가 운영하는 공유 서버라 SLA가 없고
 * 느리거나 일시적으로 안 될 수 있어, Tmap 다음 순위의 폴백으로만 사용한다.
 */
@Slf4j
@Component
public class OsrmWalkingRouteClient {

	private final RestClient restClient;

	public OsrmWalkingRouteClient() {
		this.restClient = RestClient.builder()
			.baseUrl("http://router.project-osrm.org")
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
