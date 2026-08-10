package com.chamjima.backend.geocoding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KakaoGeocodingClientTest {

	@Test
	@DisplayName("서울 도로명 주소에서 자치구를 뽑는다")
	void extractsDistrictFromSeoulRoadAddress() {
		assertThat(KakaoGeocodingClient.extractDistrict("서울특별시 은평구 은평로 240, 상가동 지1층 비109호"))
			.isEqualTo("은평구");
		assertThat(KakaoGeocodingClient.extractDistrict("서울특별시 강동구 양재대로 1360, 포레온스테이션5 2058호 (둔촌동)"))
			.isEqualTo("강동구");
		assertThat(KakaoGeocodingClient.extractDistrict("서울특별시 양천구 안양천로 657(신정동)"))
			.isEqualTo("양천구");
	}

	@Test
	@DisplayName("시도 토큰(서울특별시)을 자치구로 오인하지 않는다")
	void doesNotMistakeProvinceForDistrict() {
		assertThat(KakaoGeocodingClient.extractDistrict("서울특별시 중구 세종대로 110"))
			.isEqualTo("중구");
		assertThat(KakaoGeocodingClient.extractDistrict("경기도 성남시 분당구 판교역로 235"))
			.isEqualTo("성남시");
	}

	@Test
	@DisplayName("자치구가 없으면 도로명을 자치구로 오인하지 않고 null을 돌려준다")
	void returnsNullWhenNoDistrict() {
		assertThat(KakaoGeocodingClient.extractDistrict("세종대로 110 어딘가 건물")).isNull();
		assertThat(KakaoGeocodingClient.extractDistrict("")).isNull();
		assertThat(KakaoGeocodingClient.extractDistrict(null)).isNull();
	}

	@Test
	@DisplayName("서울 자치구는 정확 일치로만 통과한다")
	void matchesSeoulDistrictExactly() {
		assertThat(KakaoGeocodingClient.matchesDistrict("은평구", "은평구")).isTrue();
		assertThat(KakaoGeocodingClient.matchesDistrict("은평구", "송파구")).isFalse();
		assertThat(KakaoGeocodingClient.matchesDistrict("은평구", null)).isFalse();
	}

	@Test
	@DisplayName("일반시 산하 자치구('성남시 분당구')는 앞부분 일치로 통과한다")
	void matchesNestedDistrictByPrefix() {
		assertThat(KakaoGeocodingClient.matchesDistrict("성남시", "성남시 분당구")).isTrue();
		// 앞부분이 겹치기만 하는 다른 시는 통과하면 안 된다.
		assertThat(KakaoGeocodingClient.matchesDistrict("성남시", "성남시흥구")).isFalse();
		assertThat(KakaoGeocodingClient.matchesDistrict("고양시", "성남시 분당구")).isFalse();
	}

	@Test
	@DisplayName("주소 뒷부분의 건물명은 자치구 판별 범위에 들어오지 않는다")
	void ignoresTrailingTokensBeyondScanLimit() {
		// 4번째 토큰 이후에 '구'로 끝나는 단어가 있어도 무시한다.
		assertThat(KakaoGeocodingClient.extractDistrict("서울특별시 노원구 동일로 1234 상계주공")).isEqualTo("노원구");
		assertThat(KakaoGeocodingClient.extractDistrict("세종대로 110 서울시청 별관구")).isNull();
	}
}
