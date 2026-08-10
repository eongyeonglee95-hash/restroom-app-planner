package com.chamjima.backend.service;

import java.util.ArrayList;
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
		List<TipPlaceResponse> tips = CATEGORY_TIPS.keySet().stream()
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

		return deduplicate(tips);
	}

	/**
	 * 카카오가 같은 가게를 표기만 다르게 두 번 등록해 둔 경우를 하나로 합친다
	 * (예: "% 아라비카 여의도 63빌딩점"과 "퍼센트아라비카 여의도63빌딩점" — 장소 ID만 다르고
	 * 주소·좌표·카테고리가 소수점까지 동일). 카카오가 둘 다 돌려주므로 조회 시점에 거른다.
	 * 팁 레이어는 DB에 저장하지 않아서(실시간 조회) 화장실처럼 canonical_id를 쓸 수 없다.
	 *
	 * <p>좌표만으로 합치지 않는다 — 같은 건물의 서로 다른 지점이 같은 좌표를 갖는 경우가
	 * 있어서(실측: "데블다이스 강남4호점"과 "데블다이스 강남시티점") 멀쩡한 두 곳이 하나로
	 * 합쳐진다. 화장실의 노량진역(1)/(2)/(3)과 같은 함정이다. 그래서 카테고리·좌표가 같은
	 * 것에 더해 <b>한쪽 이름이 다른 쪽에 포함될 때만</b> 동일 장소로 본다.
	 */
	static List<TipPlaceResponse> deduplicate(List<TipPlaceResponse> tips) {
		Map<String, List<TipPlaceResponse>> byPlace = new LinkedHashMap<>();
		for (TipPlaceResponse tip : tips) {
			String key = tip.category() + "|" + tip.latitude() + "|" + tip.longitude();
			byPlace.computeIfAbsent(key, k -> new ArrayList<>()).add(tip);
		}

		List<TipPlaceResponse> result = new ArrayList<>();
		for (List<TipPlaceResponse> group : byPlace.values()) {
			for (int i = 0; i < group.size(); i++) {
				boolean superseded = false;
				for (int j = 0; j < group.size() && !superseded; j++) {
					superseded = i != j && supersedes(group.get(j), group.get(i), j < i);
				}
				if (!superseded) {
					result.add(group.get(i));
				}
			}
		}
		return result;
	}

	/**
	 * {@code winner}가 {@code loser}를 대체하는가. 정규화한 이름이 서로 포함 관계일 때, 더 짧은
	 * 쪽을 남긴다 — 긴 쪽은 브랜드 마크를 풀어 쓴 표기라("%"→"퍼센트") 짧은 쪽이 간판에
	 * 적힌 공식 표기에 가깝다. 길이가 같으면 앞선 항목을 남겨 순서를 안정적으로 유지한다.
	 */
	private static boolean supersedes(TipPlaceResponse winner, TipPlaceResponse loser, boolean winnerComesFirst) {
		String w = normalizeName(winner.name());
		String l = normalizeName(loser.name());
		if (w.isEmpty() || l.isEmpty() || !l.contains(w)) {
			return false;
		}
		if (w.length() != l.length()) {
			return w.length() < l.length();
		}
		// 이름이 사실상 같은 경우. 먼저 나온 쪽만 남겨 목록 순서를 흔들지 않는다.
		return winnerComesFirst;
	}

	/** 공백과 기호를 걷어내 표기 차이를 흡수한다. "% 아라비카 여의도 63빌딩점" -> "아라비카여의도63빌딩점" */
	static String normalizeName(String name) {
		if (name == null) {
			return "";
		}
		return name.replaceAll("[^\\p{IsHangul}\\p{IsAlphabetic}\\p{IsDigit}]", "").toLowerCase();
	}
}
