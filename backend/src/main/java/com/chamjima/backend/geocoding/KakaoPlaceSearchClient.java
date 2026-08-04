package com.chamjima.backend.geocoding;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoPlaceSearchClient {

	public record Place(String name, String address, double lat, double lng) {
	}

	private final RestClient restClient;
	private final String restApiKey;

	public KakaoPlaceSearchClient(@Value("${kakao.rest-api-key}") String restApiKey) {
		this.restApiKey = restApiKey;
		this.restClient = RestClient.builder()
			.baseUrl("https://dapi.kakao.com")
			.defaultHeader("Authorization", "KakaoAK " + restApiKey)
			.build();
	}

	@SuppressWarnings("unchecked")
	public List<Place> searchCategory(String categoryGroupCode, double lat, double lng, int radiusMeters, int size) {
		Map<String, Object> response = restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/v2/local/search/category.json")
				.queryParam("category_group_code", categoryGroupCode)
				.queryParam("x", lng)
				.queryParam("y", lat)
				.queryParam("radius", radiusMeters)
				.queryParam("size", size)
				.queryParam("sort", "distance")
				.build())
			.retrieve()
			.body(Map.class);

		if (response == null) {
			return List.of();
		}

		List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
		if (documents == null) {
			return List.of();
		}

		return documents.stream()
			.map(doc -> new Place(
				(String) doc.get("place_name"),
				(String) doc.get("road_address_name"),
				Double.parseDouble((String) doc.get("y")),
				Double.parseDouble((String) doc.get("x"))
			))
			.toList();
	}
}
