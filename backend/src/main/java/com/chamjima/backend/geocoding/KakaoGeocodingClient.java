package com.chamjima.backend.geocoding;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoGeocodingClient {

	public record LatLng(double lat, double lng) {
	}

	private final RestClient restClient;
	private final String restApiKey;

	public KakaoGeocodingClient(@Value("${kakao.rest-api-key}") String restApiKey) {
		this.restApiKey = restApiKey;
		this.restClient = RestClient.builder()
			.baseUrl("https://dapi.kakao.com")
			.defaultHeader("Authorization", "KakaoAK " + restApiKey)
			.build();
	}

	@SuppressWarnings("unchecked")
	public Optional<LatLng> geocode(String address) {
		if (restApiKey == null || restApiKey.isBlank()) {
			throw new IllegalStateException("kakao.rest-api-key가 설정되지 않았습니다");
		}

		Map<String, Object> response = restClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/v2/local/search/address.json")
				.queryParam("query", address)
				.build())
			.retrieve()
			.body(Map.class);

		if (response == null) {
			return Optional.empty();
		}

		List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
		if (documents == null || documents.isEmpty()) {
			return Optional.empty();
		}

		Map<String, Object> first = documents.get(0);
		double lng = Double.parseDouble((String) first.get("x"));
		double lat = Double.parseDouble((String) first.get("y"));
		return Optional.of(new LatLng(lat, lng));
	}
}
