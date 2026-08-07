package com.chamjima.backend.importer;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import com.chamjima.backend.domain.Pharmacy;
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
 * 받아와 pharmacies 테이블에 적재("전체" 레이어). 좌표는 API가 이미 제공해서 별도
 * geocoding이 필요 없다(화장실 CSV와의 차이점).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.import.pharmacies.enabled", havingValue = "true")
public class PharmacyImportRunner implements CommandLineRunner {

	private static final int PAGE_SIZE = 1000;

	private final PharmacyRepository pharmacyRepository;
	private final RestClient restClient = RestClient.create("http://openapi.seoul.go.kr:8088");

	@Value("${seoul.pharmacy-api-key}")
	private String apiKey;

	@Override
	@SuppressWarnings("unchecked")
	public void run(String... args) {
		int imported = 0;
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

			for (Map<String, Object> row : rows) {
				String externalId = (String) row.get("HPID");
				Pharmacy pharmacy = pharmacyRepository.findByExternalId(externalId).orElseGet(Pharmacy::new);
				pharmacy.setExternalId(externalId);
				pharmacy.setName((String) row.get("DUTYNAME"));
				pharmacy.setAddress((String) row.get("DUTYADDR"));
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

				pharmacyRepository.save(pharmacy);
				imported++;
			}

			log.info("진행 중... {}/{}건 처리", imported, totalCount);
			start += PAGE_SIZE;
		}

		log.info("약국 데이터 적재 완료: 총 {}건", imported);
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
