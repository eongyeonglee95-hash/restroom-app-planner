package com.chamjima.backend.importer;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import com.chamjima.backend.domain.Restroom;
import com.chamjima.backend.geocoding.KakaoGeocodingClient;
import com.chamjima.backend.repository.RestroomRepository;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.import.restrooms.enabled", havingValue = "true")
public class RestroomCsvImportRunner implements CommandLineRunner {

	private static final Charset CP949 = Charset.forName("CP949");

	private final RestroomRepository restroomRepository;
	private final KakaoGeocodingClient geocodingClient;

	@Value("${app.import.restrooms.path:data/공중화장실정보_서울특별시.csv}")
	private String csvPath;

	@Override
	public void run(String... args) throws Exception {
		int imported = 0;
		int geocodeFailed = 0;

		try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(csvPath), CP949))) {
			String[] header = reader.readNext();
			log.info("CSV 컬럼 수: {}", header.length);

			String[] row;
			while ((row = reader.readNext()) != null) {
				String externalId = row[1];
				String category = row[2];
				String name = row[4];
				String roadAddress = row[5];
				String lotAddress = row[6];
				String openHoursType = row[18];
				String openHoursDetail = row[19];
				boolean hasDiaperTable = "Y".equals(row[27]);

				String address = !roadAddress.isBlank() ? roadAddress : lotAddress;
				String openHours = openHoursDetail.isBlank() ? openHoursType : openHoursType + " " + openHoursDetail;

				Restroom restroom = restroomRepository.findByExternalId(externalId).orElseGet(Restroom::new);
				restroom.setExternalId(externalId);
				restroom.setCategory(category);
				restroom.setName(name);
				restroom.setAddress(address);
				restroom.setOpenHours(openHours);
				restroom.setHasDiaperTable(hasDiaperTable);
				restroom.setStatus(Restroom.Status.ACTIVE);

				if (restroom.getLatitude() == null) {
					try {
						geocodingClient.geocode(address).ifPresentOrElse(
							latLng -> {
								restroom.setLatitude(latLng.lat());
								restroom.setLongitude(latLng.lng());
							},
							() -> log.warn("좌표 변환 실패 (결과 없음): {} / {}", name, address));
					} catch (Exception e) {
						geocodeFailed++;
						log.warn("좌표 변환 중 오류: {} / {} - {}", name, address, e.getMessage());
					}
				}

				restroomRepository.save(restroom);
				imported++;

				if (imported % 200 == 0) {
					log.info("진행 중... {}건 처리", imported);
				}
			}
		}

		log.info("화장실 데이터 적재 완료: 총 {}건, 좌표 변환 실패 {}건", imported, geocodeFailed);
	}
}
