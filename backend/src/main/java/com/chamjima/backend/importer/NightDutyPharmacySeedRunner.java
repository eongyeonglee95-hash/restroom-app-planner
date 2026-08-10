package com.chamjima.backend.importer;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import com.chamjima.backend.domain.NightDutyPharmacy;
import com.chamjima.backend.geocoding.KakaoGeocodingClient;
import com.chamjima.backend.repository.NightDutyPharmacyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 서울시 공공심야약국(24개 자치구 38개소, 2026-08 서울시 발표 기준) "야간" 레이어 시드
 * 데이터. 전용 API가 없어 시 발표 자료(news.seoul.go.kr)를 수동으로 옮겨 담았다.
 * 좌표는 화장실 CSV 임포트와 동일하게 카카오 주소검색으로 채운다. 시가 새로 발표하면
 * 이 목록을 다시 갱신해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.import.night-duty-pharmacies.enabled", havingValue = "true")
public class NightDutyPharmacySeedRunner implements CommandLineRunner {

	private static final LocalTime NIGHT_START = LocalTime.of(22, 0);
	private static final LocalTime NIGHT_END = LocalTime.of(1, 0);
	private static final List<String> DAY_ORDER = List.of("월", "화", "수", "목", "금", "토", "일");

	private record Seed(int seq, String name, String address, String phone, String days) {
	}

	// 연번, 약국명, 주소(구 포함, "서울특별시" 접두는 geocoding 시 붙임), 전화번호, 운영일
	private static final List<Seed> SEEDS = List.of(
		new Seed(1, "세종약국", "종로구 세종대로23길 54, 지1층", "02-3210-2292", "365일"),
		new Seed(2, "대풍약국", "중구 동호로11길 43", "02-2252-3944", "월,토,일"),
		new Seed(3, "유쾌한약국", "중구 다산로 168, 성원빌딩 1층", "02-2234-0827", "화,수,목,금"),
		new Seed(4, "수복약국", "용산구 새창로12길 12", "02-714-0667", "365일"),
		new Seed(5, "새인선약국", "성동구 광나루로11길 41-1", "02-466-4159", "365일"),
		new Seed(6, "광진365플러스약국", "광진구 천호대로 566, 동우빌딩 1층", "02-455-3655", "365일"),
		new Seed(7, "보림약국", "동대문구 장한로 115", "02-2248-2082", "365일"),
		new Seed(8, "건명약국", "중랑구 면목로 421", "02-435-3900", "화,목,일"),
		new Seed(9, "엠약국", "중랑구 용마산로 670, 지1층 AB112호", "02-491-7772", "월,수,금,토"),
		new Seed(10, "모아약국", "성북구 화랑로 11길 23, 1층", "02-942-4534", "월,수,금"),
		new Seed(11, "온누리민우약국", "성북구 돌곶이로 22길 49", "02-969-4582", "365일"),
		new Seed(12, "인수약국", "강북구 덕릉로 17, 1층", "02-993-7378", "365일"),
		new Seed(13, "은혜약국", "도봉구 도봉로118길 11", "02-992-9191", "월-수"),
		new Seed(14, "새보은약국", "도봉구 삼양로154길 42", "02-996-7360", "목-일"),
		new Seed(15, "진약국", "노원구 노해로452, 노빌리안빌딩 101호", "02-935-8266", "365일"),
		new Seed(16, "새고운약국", "노원구 동일로 1003, 1층", "02-973-9286", "365일"),
		new Seed(17, "정문온누리약국", "은평구 진흥로 196", "02-549-3003", "월-금,일"),
		new Seed(18, "청룡중앙약국", "은평구 가좌로 233, 연희타워 103호", "02-303-6550", "월-토"),
		new Seed(19, "은하약국", "서대문구 명지대3길 1", "02-372-1513", "365일"),
		new Seed(20, "대유약국", "서대문구 모래내로 359", "02-379-4415", "365일"),
		new Seed(21, "셀약국", "마포구 양화로72, 118호", "02-337-7959", "365일"),
		new Seed(22, "비온뒤숲속약국", "마포구 월드컵로 111, 1층", "02-332-3250", "365일"),
		new Seed(23, "매일열린약국", "양천구 신월로 280, 1층 101호", "02-2693-3650", "365일"),
		new Seed(24, "365열린약국", "강서구 공항대로41길 52, 버들빌딩 1층", "02-3661-3650", "365일"),
		new Seed(25, "365정약국", "강서구 양천로 570, NH서울타워 1층 115호", "02-2013-5811", "금,토,일"),
		new Seed(26, "삼화약국", "금천구 은행나무로12-1", "02-803-4167", "365일"),
		new Seed(27, "세종로약국", "영등포구 여의대방로 197", "02-846-0072", "365일"),
		new Seed(28, "씨에이(CA)정문약국", "동작구 흑석로 108", "02-825-1122", "365일"),
		new Seed(29, "바다의별약국", "동작구 상도로 168, 1층", "02-821-3130", "금,토,일"),
		new Seed(30, "종로늘푸른약국", "관악구 신림로 137, 1층", "02-885-0074", "365일"),
		new Seed(31, "장수알파약국", "서초구 서초중앙로 42", "02-3487-4233", "365일"),
		new Seed(32, "강남백년가약국", "서초구 효령로 431, 청화오피스텔 102호", "02-597-0119", "365일"),
		new Seed(33, "노바약국", "강남구 강남대로118길 51, 1층", "02-542-3696", "365일"),
		new Seed(34, "제일그랜드약국", "강남구 강남대로 478, 1층", "02-546-0093", "365일"),
		new Seed(35, "시온약국", "송파구 백제고분로27길 19, 1층", "02-417-1365", "365일"),
		new Seed(36, "햇살약국", "송파구 위례성대로20길 31, 1층", "02-406-5258", "365일"),
		new Seed(37, "강동365약국", "강동구 양재대로 1574, 101호", "02-429-0365", "365일"),
		new Seed(38, "대자연약국", "강동구 구천면로 291", "02-474-7576", "365일")
	);

	private final NightDutyPharmacyRepository nightDutyPharmacyRepository;
	private final KakaoGeocodingClient geocodingClient;

	/**
	 * 좌표를 매번 다시 계산할지 여부. 38건뿐이라 비용이 사실상 없어서 기본값이 true다.
	 * false면 좌표가 비어 있는 행만 채우므로, 한번 잘못 들어간 좌표가 영원히 남는다.
	 */
	@Value("${app.import.night-duty-pharmacies.refresh-coordinates:true}")
	private boolean refreshCoordinates;

	@Override
	public void run(String... args) {
		int imported = 0;
		int geocodeFailed = 0;

		for (Seed seed : SEEDS) {
			String externalId = "SNP-%02d".formatted(seed.seq());
			String fullAddress = "서울특별시 " + seed.address();
			boolean[] days = parseDays(seed.days());

			NightDutyPharmacy pharmacy = nightDutyPharmacyRepository.findByExternalId(externalId)
				.orElseGet(NightDutyPharmacy::new);
			pharmacy.setExternalId(externalId);
			pharmacy.setName(seed.name());
			pharmacy.setAddress(fullAddress);
			pharmacy.setPhone(seed.phone());
			pharmacy.setOperatesMon(days[0]);
			pharmacy.setOperatesTue(days[1]);
			pharmacy.setOperatesWed(days[2]);
			pharmacy.setOperatesThu(days[3]);
			pharmacy.setOperatesFri(days[4]);
			pharmacy.setOperatesSat(days[5]);
			pharmacy.setOperatesSun(days[6]);
			pharmacy.setNightStart(NIGHT_START);
			pharmacy.setNightEnd(NIGHT_END);
			pharmacy.setStatus(NightDutyPharmacy.Status.ACTIVE);

			if (refreshCoordinates || pharmacy.getLatitude() == null) {
				try {
					geocodingClient.geocode(fullAddress).ifPresentOrElse(
						latLng -> {
							pharmacy.setLatitude(latLng.lat());
							pharmacy.setLongitude(latLng.lng());
						},
						() -> log.warn("좌표 변환 실패 (결과 없음): {} / {}", seed.name(), fullAddress));
				} catch (Exception e) {
					geocodeFailed++;
					log.warn("좌표 변환 중 오류: {} / {} - {}", seed.name(), fullAddress, e.getMessage());
				}
			}

			nightDutyPharmacyRepository.save(pharmacy);
			imported++;
		}

		log.info("공공심야약국 시드 데이터 적재 완료: 총 {}건, 좌표 변환 실패 {}건", imported, geocodeFailed);
	}

	private boolean[] parseDays(String spec) {
		boolean[] days = new boolean[7];
		if ("365일".equals(spec)) {
			Arrays.fill(days, true);
			return days;
		}
		for (String token : spec.split(",")) {
			token = token.trim();
			if (token.contains("-")) {
				String[] parts = token.split("-");
				int start = DAY_ORDER.indexOf(parts[0]);
				int end = DAY_ORDER.indexOf(parts[1]);
				for (int i = start; i <= end; i++) {
					days[i] = true;
				}
			} else {
				int idx = DAY_ORDER.indexOf(token);
				if (idx >= 0) {
					days[idx] = true;
				}
			}
		}
		return days;
	}
}
