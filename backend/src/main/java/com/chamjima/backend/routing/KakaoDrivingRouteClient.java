package com.chamjima.backend.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.chamjima.backend.routing.TmapWalkingRouteClient.PathPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 카카오모빌리티는 도보 길찾기를 REST API로 제공하지 않아서, Tmap 호출이 실패했을 때
 * 대체용으로 자동차 경로를 대신 사용한다. 실제 도로를 따라가므로 최소한 건물을 뚫고
 * 지나가는 직선거리보다는 현실적인 거리/경로를 얻을 수 있음 (소요시간은 자동차 기준이라
 * 쓰지 않고, 거리만 가져와서 도보 속도로 환산한다).
 */
@Component
public class KakaoDrivingRouteClient {

	public record RoadRoute(int distanceMeters, List<PathPoint> path) {
	}

	private final RestClient restClient;

	public KakaoDrivingRouteClient(@Value("${kakao.rest-api-key}") String restApiKey) {
		this.restClient = RestClient.builder()
			.baseUrl("https://apis-navi.kakaomobility.com")
			.defaultHeader("Authorization", "KakaoAK " + restApiKey)
			.build();
	}

	@SuppressWarnings("unchecked")
	public Optional<RoadRoute> route(double startLat, double startLng, double endLat, double endLng) {
		Map<String, Object> response;
		try {
			response = restClient.get()
				.uri(uriBuilder -> uriBuilder
					.path("/v1/directions")
					.queryParam("origin", startLng + "," + startLat)
					.queryParam("destination", endLng + "," + endLat)
					.queryParam("priority", "RECOMMEND")
					.build())
				.retrieve()
				.body(Map.class);
		} catch (Exception e) {
			return Optional.empty();
		}

		if (response == null) {
			return Optional.empty();
		}

		List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
		if (routes == null || routes.isEmpty()) {
			return Optional.empty();
		}

		Map<String, Object> route = routes.get(0);
		if (!Integer.valueOf(0).equals(route.get("result_code"))) {
			return Optional.empty();
		}

		Map<String, Object> summary = (Map<String, Object>) route.get("summary");
		int distanceMeters = ((Number) summary.get("distance")).intValue();

		List<PathPoint> path = new ArrayList<>();
		List<Map<String, Object>> sections = (List<Map<String, Object>>) route.get("sections");
		for (Map<String, Object> section : sections) {
			List<Map<String, Object>> roads = (List<Map<String, Object>>) section.get("roads");
			for (Map<String, Object> road : roads) {
				List<Number> vertexes = (List<Number>) road.get("vertexes");
				for (int i = 0; i + 1 < vertexes.size(); i += 2) {
					double lng = vertexes.get(i).doubleValue();
					double lat = vertexes.get(i + 1).doubleValue();
					path.add(new PathPoint(lat, lng));
				}
			}
		}

		return Optional.of(new RoadRoute(distanceMeters, path));
	}
}
