package com.chamjima.backend.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TmapWalkingRouteClient {

	public record PathPoint(double lat, double lng) {
	}

	public record WalkingRoute(int totalTimeSeconds, int totalDistanceMeters, List<PathPoint> path) {
	}

	private final RestClient restClient;
	private final String appKey;

	public TmapWalkingRouteClient(@Value("${tmap.app-key}") String appKey) {
		this.appKey = appKey;
		this.restClient = RestClient.builder()
			.baseUrl("https://apis.openapi.sk.com")
			.defaultHeader("appKey", appKey)
			.build();
	}

	@SuppressWarnings("unchecked")
	public Optional<WalkingRoute> route(double startLat, double startLng, double endLat, double endLng) {
		if (appKey == null || appKey.isBlank()) {
			throw new IllegalStateException("tmap.app-key가 설정되지 않았습니다");
		}

		Map<String, Object> body = Map.of(
			"startX", startLng,
			"startY", startLat,
			"endX", endLng,
			"endY", endLat,
			"startName", "start",
			"endName", "end"
		);

		Map<String, Object> response = restClient.post()
			.uri("/tmap/routes/pedestrian?version=1")
			.body(body)
			.retrieve()
			.body(Map.class);

		if (response == null) {
			return Optional.empty();
		}

		List<Map<String, Object>> features = (List<Map<String, Object>>) response.get("features");
		if (features == null || features.isEmpty()) {
			return Optional.empty();
		}

		Map<String, Object> properties = (Map<String, Object>) features.get(0).get("properties");
		if (properties == null || properties.get("totalTime") == null) {
			return Optional.empty();
		}

		int totalTime = ((Number) properties.get("totalTime")).intValue();
		int totalDistance = ((Number) properties.get("totalDistance")).intValue();
		List<PathPoint> path = extractPath(features);
		return Optional.of(new WalkingRoute(totalTime, totalDistance, path));
	}

	@SuppressWarnings("unchecked")
	private List<PathPoint> extractPath(List<Map<String, Object>> features) {
		List<PathPoint> path = new ArrayList<>();

		for (Map<String, Object> feature : features) {
			Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
			if (geometry == null || !"LineString".equals(geometry.get("type"))) {
				continue;
			}

			List<List<Number>> coordinates = (List<List<Number>>) geometry.get("coordinates");
			for (List<Number> coordinate : coordinates) {
				double lng = coordinate.get(0).doubleValue();
				double lat = coordinate.get(1).doubleValue();
				path.add(new PathPoint(lat, lng));
			}
		}

		return path;
	}
}
