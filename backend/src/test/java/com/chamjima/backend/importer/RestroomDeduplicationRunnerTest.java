package com.chamjima.backend.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.chamjima.backend.domain.Restroom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestroomDeduplicationRunnerTest {

	private static Restroom restroom(String name, String address, Double lat, Double lng) {
		Restroom restroom = new Restroom();
		restroom.setName(name);
		restroom.setAddress(address);
		restroom.setLatitude(lat);
		restroom.setLongitude(lng);
		return restroom;
	}

	@Test
	@DisplayName("이름과 좌표가 같으면 주소 표기가 달라도 같은 그룹으로 묶는다")
	void groupsSamePlaceDespiteAddressFormatting() {
		// 실제 데이터: 괄호 안 건물명 유무만 다른 같은 장소가 41그룹 있다.
		Restroom withBuilding = restroom("기독교TV멀티미디어센터",
			"서울특별시 동작구 노량진로 100 (노량진동, 기독교TV멀티미디어센터)", 37.51, 126.94);
		Restroom withoutBuilding = restroom("기독교TV멀티미디어센터",
			"서울특별시 동작구 노량진로 100 (노량진동)", 37.51, 126.94);

		assertThat(RestroomDeduplicationRunner.groupKey(withBuilding))
			.isEqualTo(RestroomDeduplicationRunner.groupKey(withoutBuilding));
	}

	@Test
	@DisplayName("좌표가 같아도 이름이 다르면 별개 화장실로 둔다")
	void keepsDistinctNamesApartAtSameCoordinates() {
		// 실제 데이터: 노량진역(1)(2)(3)은 좌표가 같지만 출구별로 다른 화장실이다.
		Restroom exit1 = restroom("노량진역(1)", "서울특별시 동작구", 37.5135735646312, 126.940826454376);
		Restroom exit2 = restroom("노량진역(2)", "서울특별시 동작구", 37.5135735646312, 126.940826454376);
		Restroom exit3 = restroom("노량진역(3)", "서울특별시 동작구", 37.5135735646312, 126.940826454376);

		assertThat(RestroomDeduplicationRunner.groupKey(exit1))
			.isNotEqualTo(RestroomDeduplicationRunner.groupKey(exit2))
			.isNotEqualTo(RestroomDeduplicationRunner.groupKey(exit3));
	}

	@Test
	@DisplayName("이름이 같아도 좌표가 다르면 별개 화장실로 둔다")
	void keepsSameNameApartAtDifferentCoordinates() {
		Restroom gangnam = restroom("공원화장실", "서울특별시 강남구", 37.49, 127.02);
		Restroom eunpyeong = restroom("공원화장실", "서울특별시 은평구", 37.62, 126.91);

		assertThat(RestroomDeduplicationRunner.groupKey(gangnam))
			.isNotEqualTo(RestroomDeduplicationRunner.groupKey(eunpyeong));
	}

	@Test
	@DisplayName("이름 앞뒤 공백은 그룹 판정에 영향을 주지 않는다")
	void trimsNameBeforeGrouping() {
		assertThat(RestroomDeduplicationRunner.groupKey(restroom(" 남일교회 ", "주소", 37.5, 127.0)))
			.isEqualTo(RestroomDeduplicationRunner.groupKey(restroom("남일교회", "주소", 37.5, 127.0)));
	}

	@Test
	@DisplayName("좌표가 없으면 그룹에서 제외한다")
	void skipsRowsWithoutCoordinates() {
		assertThat(RestroomDeduplicationRunner.groupKey(restroom("이름", "주소", null, null))).isNull();
		assertThat(RestroomDeduplicationRunner.groupKey(restroom("이름", "주소", 37.5, null))).isNull();
		assertThat(RestroomDeduplicationRunner.groupKey(restroom(null, "주소", 37.5, 127.0))).isNull();
	}
}
