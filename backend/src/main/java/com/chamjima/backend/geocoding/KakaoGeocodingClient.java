package com.chamjima.backend.geocoding;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class KakaoGeocodingClient {

	public record LatLng(double lat, double lng) {
	}

	/** 시도 토큰. "서울특별시"도 "시"로 끝나서 시군구 판별 시 먼저 걸러내야 한다. */
	private static final List<String> PROVINCE_SUFFIXES =
		List.of("특별시", "광역시", "특별자치시", "특별자치도");

	/** 시군구는 주소 앞부분에 온다. 뒤쪽 도로명/건물명을 시군구로 오인하지 않도록 범위를 제한한다. */
	private static final int DISTRICT_SCAN_LIMIT = 3;

	private final RestClient restClient;
	private final String restApiKey;

	public KakaoGeocodingClient(@Value("${kakao.rest-api-key}") String restApiKey) {
		this.restApiKey = restApiKey;
		this.restClient = RestClient.builder()
			.baseUrl("https://dapi.kakao.com")
			.defaultHeader("Authorization", "KakaoAK " + restApiKey)
			.build();
	}

	/**
	 * 주소를 좌표로 변환한다.
	 *
	 * <p>카카오는 후보를 여러 건 돌려줄 수 있고, 그중 첫 건이 입력 주소와 다른 시군구인 경우가
	 * 있다. 그대로 쓰면 엉뚱한 구의 좌표가 조용히 저장되므로(실패는 로그에 남지만 오매칭은
	 * 아무 흔적도 남지 않는다), 입력 주소에서 시군구를 뽑아 후보를 검증한다. 일치하는 후보가
	 * 하나도 없으면 좌표를 채우는 대신 실패로 처리한다 — 틀린 좌표는 좌표가 없는 것보다 나쁘다.
	 */
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

		String expectedDistrict = extractDistrict(address);
		if (expectedDistrict == null) {
			if (documents.size() > 1) {
				log.warn("시군구를 판별할 수 없어 첫 후보 사용: {} (후보 {}건)", address, documents.size());
			}
			return toLatLng(documents.get(0));
		}

		for (Map<String, Object> document : documents) {
			if (matchesDistrict(expectedDistrict, districtOf(document))) {
				return toLatLng(document);
			}
		}

		log.warn("시군구 불일치로 좌표 폐기: {} (기대: {}, 후보 {}건: {})",
			address, expectedDistrict, documents.size(), districtsOf(documents));
		return Optional.empty();
	}

	/** "서울특별시 은평구 은평로 240, ..." -> "은평구". 판별 불가 시 null. */
	public static String extractDistrict(String address) {
		if (address == null || address.isBlank()) {
			return null;
		}
		String[] tokens = address.trim().split("\\s+");
		int scanned = 0;
		for (String token : tokens) {
			if (scanned >= DISTRICT_SCAN_LIMIT) {
				break;
			}
			scanned++;
			if (isProvince(token)) {
				continue;
			}
			if (token.endsWith("구") || token.endsWith("군") || token.endsWith("시")) {
				return token;
			}
		}
		return null;
	}

	/**
	 * 카카오의 region_2depth_name은 서울이면 "은평구"지만, 일반시 산하 자치구는
	 * "성남시 분당구"처럼 두 단계가 합쳐져 온다. 주소에서 뽑은 "성남시"와 정확 일치로
	 * 비교하면 정상 결과까지 버리게 되므로 앞부분 일치도 허용한다.
	 */
	static boolean matchesDistrict(String expected, String actual) {
		if (expected == null || actual == null) {
			return false;
		}
		return expected.equals(actual) || actual.startsWith(expected + " ");
	}

	private static boolean isProvince(String token) {
		if (token.endsWith("도") && token.length() <= 4) {
			return true;
		}
		return PROVINCE_SUFFIXES.stream().anyMatch(token::endsWith);
	}

	@SuppressWarnings("unchecked")
	private static String districtOf(Map<String, Object> document) {
		for (String key : List.of("road_address", "address")) {
			Map<String, Object> part = (Map<String, Object>) document.get(key);
			if (part == null) {
				continue;
			}
			if (part.get("region_2depth_name") instanceof String region && !region.isBlank()) {
				return region;
			}
		}
		return null;
	}

	private static Set<String> districtsOf(List<Map<String, Object>> documents) {
		Set<String> districts = new LinkedHashSet<>();
		for (Map<String, Object> document : documents) {
			String district = districtOf(document);
			districts.add(district == null ? "(불명)" : district);
		}
		return districts;
	}

	private static Optional<LatLng> toLatLng(Map<String, Object> document) {
		if (!(document.get("x") instanceof String lngText) || !(document.get("y") instanceof String latText)) {
			return Optional.empty();
		}
		try {
			return Optional.of(new LatLng(Double.parseDouble(latText), Double.parseDouble(lngText)));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
