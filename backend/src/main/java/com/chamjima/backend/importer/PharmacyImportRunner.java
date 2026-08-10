package com.chamjima.backend.importer;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.chamjima.backend.domain.Pharmacy;
import com.chamjima.backend.geocoding.KakaoGeocodingClient;
import com.chamjima.backend.repository.PharmacyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 서울시 약국 운영시간 정보(TbPharmacyOperateInfo) Open API를 페이지네이션으로 전부
 * 받아와 pharmacies 테이블에 적재("전체" 레이어).
 *
 * <p>좌표는 원칙적으로 API가 주는 WGS84LAT/LON을 쓴다(화장실 CSV와의 차이점). 다만 이
 * API는 서로 다른 자치구의 약국에 같은 좌표를 주는 오류가 있어서 — 2026-08 기준 174건 —
 * 적재 전에 검증한다. 같은 좌표를 공유하는 약국들의 주소가 서로 다른 자치구를 가리키면
 * 그중 최대 한 곳만 맞을 수 있으므로, 해당 좌표는 신뢰하지 않고 주소 기준으로 다시
 * geocoding한다. 같은 건물에 입주한 약국들이 좌표를 공유하는 것은 정상이므로(예: 강동구
 * 양재대로 1360 한 건물에 10곳) 자치구가 갈리는 경우만 골라낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.import.pharmacies.enabled", havingValue = "true")
public class PharmacyImportRunner implements CommandLineRunner {

	private static final int PAGE_SIZE = 1000;

	private final PharmacyRepository pharmacyRepository;
	private final KakaoGeocodingClient geocodingClient;
	private final RestClient restClient = RestClient.create("http://openapi.seoul.go.kr:8088");

	@Value("${seoul.pharmacy-api-key}")
	private String apiKey;

	@Override
	public void run(String... args) {
		List<Map<String, Object>> rows = fetchAllRows();
		if (rows.isEmpty()) {
			log.warn("약국 데이터를 한 건도 받지 못해 적재를 건너뜁니다");
			return;
		}

		Set<String> suspectIds = findSuspectExternalIds(rows);
		log.info("좌표 검증: 전체 {}건 중 자치구가 어긋난 좌표 그룹에 속한 {}건을 주소 기준으로 다시 계산합니다",
			rows.size(), suspectIds.size());

		int imported = 0;
		int repaired = 0;
		int dropped = 0;

		for (Map<String, Object> row : rows) {
			String externalId = (String) row.get("HPID");
			String address = (String) row.get("DUTYADDR");

			Pharmacy pharmacy = pharmacyRepository.findByExternalId(externalId).orElseGet(Pharmacy::new);
			pharmacy.setExternalId(externalId);
			pharmacy.setName((String) row.get("DUTYNAME"));
			pharmacy.setAddress(address);
			pharmacy.setPhone((String) row.get("DUTYTEL1"));
			pharmacy.setLatitude(parseDouble((String) row.get("WGS84LAT")));
			pharmacy.setLongitude(parseDouble((String) row.get("WGS84LON")));
			pharmacy.setMonOpen(parseTime((String) row.get("DUTYTIME1S")));
			pharmacy.setMonClose(parseTime((String) row.get("DUTYTIME1C")));
			pharmacy.setTueOpen(parseTime((String) row.get("DUTYTIME2S")));
			pharmacy.setTueClose(parseTime((String) row.get("DUTYTIME2C")));
			pharmacy.setWedOpen(parseTime((String) row.get("DUTYTIME3S")));
			pharmacy.setWedClose(parseTime((String) row.get("DUTYTIME3C")));
			pharmacy.setThuOpen(parseTime((String) row.get("DUTYTIME4S")));
			pharmacy.setThuClose(parseTime((String) row.get("DUTYTIME4C")));
			pharmacy.setFriOpen(parseTime((String) row.get("DUTYTIME5S")));
			pharmacy.setFriClose(parseTime((String) row.get("DUTYTIME5C")));
			pharmacy.setSatOpen(parseTime((String) row.get("DUTYTIME6S")));
			pharmacy.setSatClose(parseTime((String) row.get("DUTYTIME6C")));
			pharmacy.setSunOpen(parseTime((String) row.get("DUTYTIME7S")));
			pharmacy.setSunClose(parseTime((String) row.get("DUTYTIME7C")));
			pharmacy.setHolidayOpen(parseTime((String) row.get("DUTYTIME8S")));
			pharmacy.setHolidayClose(parseTime((String) row.get("DUTYTIME8C")));
			pharmacy.setStatus(Pharmacy.Status.ACTIVE);

			if (suspectIds.contains(externalId)) {
				if (regeocode(pharmacy, address)) {
					repaired++;
				} else {
					dropped++;
				}
			}

			pharmacyRepository.save(pharmacy);
			imported++;
		}

		log.info("약국 데이터 적재 완료: 총 {}건 (좌표 재계산 성공 {}건, 실패해 좌표 비움 {}건)",
			imported, repaired, dropped);
	}

	/**
	 * 의심 좌표를 주소 기준으로 다시 계산한다. 실패하면 좌표를 비운다 — 이 좌표는 이미
	 * "같은 지점에 다른 자치구 약국이 섞여 있다"는 게 확인된 값이라, 틀린 자치구로 안내하는
	 * 것보다 지도에서 빠지는 편이 낫다.
	 *
	 * @return 좌표를 새로 채웠으면 true
	 */
	private boolean regeocode(Pharmacy pharmacy, String address) {
		try {
			var latLng = geocodingClient.geocode(geocodableAddress(address));
			if (latLng.isPresent()) {
				pharmacy.setLatitude(latLng.get().lat());
				pharmacy.setLongitude(latLng.get().lng());
				return true;
			}
			log.warn("좌표 재계산 실패로 좌표를 비움: {} / {}", pharmacy.getName(), address);
		} catch (Exception e) {
			log.warn("좌표 재계산 중 오류로 좌표를 비움: {} / {} - {}", pharmacy.getName(), address, e.getMessage());
		}
		pharmacy.setLatitude(null);
		pharmacy.setLongitude(null);
		return false;
	}

	/**
	 * 같은 좌표를 쓰면서 주소의 자치구가 둘 이상으로 갈리는 그룹에 속한 약국의 external_id.
	 * 같은 건물 입주 약국끼리 좌표를 공유하는 정상 케이스는 자치구가 하나라 걸리지 않는다.
	 */
	static Set<String> findSuspectExternalIds(List<Map<String, Object>> rows) {
		Map<String, List<Map<String, Object>>> byCoordinate = new HashMap<>();
		for (Map<String, Object> row : rows) {
			String lat = (String) row.get("WGS84LAT");
			String lng = (String) row.get("WGS84LON");
			if (lat == null || lat.isBlank() || lng == null || lng.isBlank()) {
				continue;
			}
			byCoordinate.computeIfAbsent(lat + "," + lng, key -> new ArrayList<>()).add(row);
		}

		Set<String> suspects = new HashSet<>();
		for (List<Map<String, Object>> group : byCoordinate.values()) {
			if (group.size() < 2) {
				continue;
			}
			Set<String> districts = new HashSet<>();
			for (Map<String, Object> row : group) {
				String district = KakaoGeocodingClient.extractDistrict((String) row.get("DUTYADDR"));
				if (district != null) {
					districts.add(district);
				}
			}
			if (districts.size() > 1) {
				for (Map<String, Object> row : group) {
					suspects.add((String) row.get("HPID"));
				}
			}
		}
		return suspects;
	}

	/** 카카오 주소검색은 호수/건물명이 붙으면 실패율이 올라간다. 첫 쉼표 앞까지만 넘긴다. */
	static String geocodableAddress(String address) {
		if (address == null) {
			return null;
		}
		int comma = address.indexOf(',');
		return (comma > 0 ? address.substring(0, comma) : address).trim();
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> fetchAllRows() {
		List<Map<String, Object>> all = new ArrayList<>();
		int start = 1;
		int totalCount = Integer.MAX_VALUE;

		while (start <= totalCount) {
			int end = start + PAGE_SIZE - 1;
			Map<String, Object> response = restClient.get()
				.uri("/{apiKey}/json/TbPharmacyOperateInfo/{start}/{end}/", apiKey, start, end)
				.retrieve()
				.body(Map.class);

			Map<String, Object> body = response == null ? null : (Map<String, Object>) response.get("TbPharmacyOperateInfo");
			if (body == null) {
				log.warn("약국 데이터 응답 형식 오류: {}", response);
				break;
			}
			totalCount = ((Number) body.get("list_total_count")).intValue();

			List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("row");
			if (rows == null || rows.isEmpty()) {
				break;
			}

			all.addAll(rows);
			log.info("수신 중... {}/{}건", all.size(), totalCount);
			start += PAGE_SIZE;
		}

		return all;
	}

	private Double parseDouble(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private LocalTime parseTime(String value) {
		if (value == null || value.length() != 4) {
			return null;
		}
		try {
			int hour = Integer.parseInt(value.substring(0, 2)) % 24;
			int minute = Integer.parseInt(value.substring(2, 4));
			return LocalTime.of(hour, minute);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
