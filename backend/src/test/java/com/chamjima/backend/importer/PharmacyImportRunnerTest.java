package com.chamjima.backend.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PharmacyImportRunnerTest {

	private static Map<String, Object> row(String hpid, String address, String lat, String lng) {
		return Map.of("HPID", hpid, "DUTYADDR", address, "WGS84LAT", lat, "WGS84LON", lng);
	}

	@Test
	@DisplayName("같은 건물에 입주해 좌표를 공유하는 약국들은 의심 대상이 아니다")
	void sameBuildingSharedCoordinatesAreNotSuspect() {
		// 실제 데이터: 강동구 양재대로 1360 한 건물에 약국 10곳이 같은 좌표를 쓴다.
		List<Map<String, Object>> rows = List.of(
			row("A1", "서울특별시 강동구 양재대로 1360, 1층 1005호 (둔촌동)", "37.527", "127.137"),
			row("A2", "서울특별시 강동구 양재대로 1360, 2층 2071호 (둔촌동)", "37.527", "127.137"),
			row("A3", "서울특별시 강동구 양재대로 1360, 3층 3062호 (둔촌동)", "37.527", "127.137"));

		assertThat(PharmacyImportRunner.findSuspectExternalIds(rows)).isEmpty();
	}

	@Test
	@DisplayName("자치구가 다른데 좌표가 같으면 그룹 전체를 의심 대상으로 잡는다")
	void sharedCoordinatesAcrossDistrictsAreSuspect() {
		// 실제 데이터: 서초구 가온약국이 은평구 가온약국의 좌표를 그대로 받았다.
		List<Map<String, Object>> rows = List.of(
			row("B1", "서울특별시 서초구 강남대로 465, 교보타워 B동 지상1층 (서초동)", "37.6205", "126.9197"),
			row("B2", "서울특별시 은평구 통일로 871, 101호 (갈현동)", "37.6205", "126.9197"));

		assertThat(PharmacyImportRunner.findSuspectExternalIds(rows))
			.containsExactlyInAnyOrder("B1", "B2");
	}

	@Test
	@DisplayName("좌표가 겹치지 않으면 의심하지 않는다")
	void uniqueCoordinatesAreNotSuspect() {
		List<Map<String, Object>> rows = List.of(
			row("C1", "서울특별시 서초구 강남대로 465", "37.49", "127.02"),
			row("C2", "서울특별시 은평구 통일로 871", "37.62", "126.91"));

		assertThat(PharmacyImportRunner.findSuspectExternalIds(rows)).isEmpty();
	}

	@Test
	@DisplayName("좌표가 비어 있는 행은 그룹 판정에서 제외한다")
	void blankCoordinatesAreIgnored() {
		List<Map<String, Object>> rows = List.of(
			row("D1", "서울특별시 서초구 강남대로 465", "", ""),
			row("D2", "서울특별시 은평구 통일로 871", "", ""));

		assertThat(PharmacyImportRunner.findSuspectExternalIds(rows)).isEmpty();
	}

	@Test
	@DisplayName("geocoding용 주소는 첫 쉼표 앞까지만 쓴다")
	void trimsAddressDetailForGeocoding() {
		assertThat(PharmacyImportRunner.geocodableAddress(
			"서울특별시 은평구 은평로 240, 상가동 지1층 비109호 (응암동, 힐스테이트녹번역)"))
			.isEqualTo("서울특별시 은평구 은평로 240");
		// 쉼표가 없으면 그대로 둔다.
		assertThat(PharmacyImportRunner.geocodableAddress("서울특별시 금천구 은행나무로12-1"))
			.isEqualTo("서울특별시 금천구 은행나무로12-1");
		assertThat(PharmacyImportRunner.geocodableAddress(null)).isNull();
	}
}
