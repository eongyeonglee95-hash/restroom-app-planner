package com.chamjima.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import com.chamjima.backend.dto.TipPlaceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TipServiceTest {

	private static TipPlaceResponse tip(String name, String category, double lat, double lng) {
		return new TipPlaceResponse(name, category, "서울 영등포구 63로 50", lat, lng, "팁", "");
	}

	@Test
	@DisplayName("표기만 다른 같은 가게는 공식 표기(짧은 쪽)만 남긴다")
	void keepsOfficialNotationForSamePlace() {
		// 실측: 장소 ID만 다르고 주소·좌표·카테고리가 소수점까지 동일하다.
		List<TipPlaceResponse> tips = List.of(
			tip("퍼센트아라비카 여의도63빌딩점", "카페", 37.5196642456696, 126.939909903592),
			tip("% 아라비카 여의도 63빌딩점", "카페", 37.5196642456696, 126.939909903592));

		assertThat(TipService.deduplicate(tips))
			.extracting(TipPlaceResponse::name)
			.containsExactly("% 아라비카 여의도 63빌딩점");
	}

	@Test
	@DisplayName("좌표가 같아도 지점명이 다르면 둘 다 남긴다")
	void keepsDistinctBranchesAtSameCoordinates() {
		// 실측: 같은 좌표·카테고리지만 서로 다른 지점이다. 합치면 멀쩡한 곳이 사라진다.
		List<TipPlaceResponse> tips = List.of(
			tip("데블다이스 강남4호점", "카페", 37.4979, 127.0276),
			tip("데블다이스 강남시티점", "카페", 37.4979, 127.0276));

		assertThat(TipService.deduplicate(tips)).hasSize(2);
	}

	@Test
	@DisplayName("좌표가 다르면 이름이 포함 관계라도 합치지 않는다")
	void keepsPlacesApartAtDifferentCoordinates() {
		List<TipPlaceResponse> tips = List.of(
			tip("스타벅스 여의도점", "카페", 37.5196, 126.9399),
			tip("스타벅스 여의도점 2호", "카페", 37.5300, 126.9500));

		assertThat(TipService.deduplicate(tips)).hasSize(2);
	}

	@Test
	@DisplayName("카테고리가 다르면 합치지 않는다")
	void keepsPlacesApartAcrossCategories() {
		List<TipPlaceResponse> tips = List.of(
			tip("이마트24", "편의점", 37.5196, 126.9399),
			tip("이마트24 카페", "카페", 37.5196, 126.9399));

		assertThat(TipService.deduplicate(tips)).hasSize(2);
	}

	@Test
	@DisplayName("중복이 없으면 순서를 그대로 유지한다")
	void preservesOrderWhenNoDuplicates() {
		List<TipPlaceResponse> tips = List.of(
			tip("스타벅스 63Sky Picnic점", "카페", 37.5198, 126.9402),
			tip("GS25 63빌딩점", "편의점", 37.5199, 126.9403),
			tip("하드앤빈스", "카페", 37.5209, 126.9395));

		assertThat(TipService.deduplicate(tips))
			.extracting(TipPlaceResponse::name)
			.containsExactly("스타벅스 63Sky Picnic점", "GS25 63빌딩점", "하드앤빈스");
	}

	@Test
	@DisplayName("이름 정규화는 공백과 기호를 걷어낸다")
	void normalizesName() {
		assertThat(TipService.normalizeName("% 아라비카 여의도 63빌딩점")).isEqualTo("아라비카여의도63빌딩점");
		assertThat(TipService.normalizeName("퍼센트아라비카 여의도63빌딩점")).isEqualTo("퍼센트아라비카여의도63빌딩점");
		assertThat(TipService.normalizeName("스타벅스 63Sky Picnic점")).isEqualTo("스타벅스63skypicnic점");
		assertThat(TipService.normalizeName(null)).isEmpty();
	}

	@Test
	@DisplayName("이름이 완전히 같으면 하나만 남긴다")
	void collapsesIdenticalNames() {
		List<TipPlaceResponse> tips = List.of(
			tip("CU 여의도점", "편의점", 37.5196, 126.9399),
			tip("CU 여의도점", "편의점", 37.5196, 126.9399));

		assertThat(TipService.deduplicate(tips)).hasSize(1);
	}
}
