package com.chamjima.backend.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.chamjima.backend.dto.TipPlaceResponse;
import com.chamjima.backend.geocoding.KakaoPlaceSearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TipService {

	private static final int RADIUS_METERS = 500;
	private static final int SIZE_PER_CATEGORY = 5;

	private static final Map<String, String> CATEGORY_TIPS = new LinkedHashMap<>();
	private static final Map<String, String> CATEGORY_LABELS = new LinkedHashMap<>();

	static {
		CATEGORY_TIPS.put("CS2", "직원에게 물어보면 화장실을 사용할 수 있는 경우가 많아요.");
		CATEGORY_LABELS.put("CS2", "편의점");

		CATEGORY_TIPS.put("CE7", "매장 이용 손님 대상인 경우가 많지만, 급하면 직원에게 물어보세요.");
		CATEGORY_LABELS.put("CE7", "카페");

		CATEGORY_TIPS.put("MT1", "지하 1층에 화장실이 있는 경우가 많아요.");
		CATEGORY_LABELS.put("MT1", "대형마트/백화점");

		CATEGORY_TIPS.put("OL7", "화장실을 개방하는 주유소가 많아요. 직원에게 물어보세요.");
		CATEGORY_LABELS.put("OL7", "주유소");
	}

	private final KakaoPlaceSearchClient placeSearchClient;

	public List<TipPlaceResponse> findNearbyTips(double lat, double lng) {
		return CATEGORY_TIPS.keySet().stream()
			.flatMap(categoryCode -> placeSearchClient
				.searchCategory(categoryCode, lat, lng, RADIUS_METERS, SIZE_PER_CATEGORY)
				.stream()
				.map(place -> new TipPlaceResponse(
					place.name(),
					CATEGORY_LABELS.get(categoryCode),
					place.address(),
					place.lat(),
					place.lng(),
					CATEGORY_TIPS.get(categoryCode),
					place.phone()
				)))
			.toList();
	}
}
