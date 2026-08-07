package com.chamjima.backend.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import com.chamjima.backend.domain.NightDutyPharmacy;
import com.chamjima.backend.domain.Pharmacy;
import com.chamjima.backend.dto.PharmacyResponse;
import com.chamjima.backend.geo.GeoUtils;
import com.chamjima.backend.repository.NightDutyPharmacyRepository;
import com.chamjima.backend.repository.PharmacyRepository;
import com.chamjima.backend.routing.TmapWalkingRouteClient.WalkingRoute;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PharmacyService {

	public enum Layer { ALL, GENERAL, NIGHT }

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	/**
	 * "야간" 기준 시각. 정부 공식 심야 기준은 22시지만, 서울시 약국 5,510곳 기준으로 21시가
	 * 넘으면 열려있는 곳이 477곳(8.6%)까지 급감해 실제 공백은 21시부터 시작한다. 22시로
	 * 잡으면 정작 밤 9시에 야간 필터를 눌렀을 때 결과가 비어버려서 21시를 기준으로 삼는다.
	 */
	private static final LocalTime NIGHT_THRESHOLD = LocalTime.of(21, 0);

	/** 도보 경로 API 호출량(특히 Tmap 무료 할당량)을 아끼려고 상위 후보에만 경로를 계산한다. */
	private static final int ROUTE_CANDIDATE_COUNT = 8;

	private final PharmacyRepository pharmacyRepository;
	private final NightDutyPharmacyRepository nightDutyPharmacyRepository;
	private final WalkingRouteService walkingRouteService;

	private record Candidate(
		String id, String type, String name, String address, String phone,
		double latitude, double longitude, double distanceMeters, boolean openNow, String todayHours
	) {
	}

	public List<PharmacyResponse> findNearby(
		double lat, double lng, double radiusMeters, int limit, Layer layer, boolean openNowOnly
	) {
		ZonedDateTime now = ZonedDateTime.now(SEOUL);
		DayOfWeek today = now.getDayOfWeek();

		// NIGHT 레이어에서는 일반 약국 중 늦게까지 하는 곳만 남기고, 공공심야약국과 함께 보여준다.
		Stream<Candidate> general = pharmacyRepository.findByStatusAndLatitudeIsNotNull(Pharmacy.Status.ACTIVE).stream()
			.filter(p -> layer != Layer.NIGHT || opensLateEnough(p, today))
			.map(p -> toGeneralCandidate(p, lat, lng, now));

		Stream<Candidate> night = layer == Layer.GENERAL
			? Stream.empty()
			: nightDutyPharmacyRepository.findByStatusAndLatitudeIsNotNull(NightDutyPharmacy.Status.ACTIVE).stream()
				.map(p -> toNightCandidate(p, lat, lng, now));

		List<Candidate> candidates = Stream.concat(general, night)
			.filter(c -> c.distanceMeters() <= radiusMeters)
			.filter(c -> !openNowOnly || c.openNow())
			.sorted(Comparator.comparingDouble(Candidate::distanceMeters))
			.limit(Math.min(limit, ROUTE_CANDIDATE_COUNT))
			.toList();

		return candidates.stream()
			.map(c -> withWalkingRoute(c, lat, lng))
			.sorted(Comparator.comparingInt(PharmacyResponse::walkingTimeSeconds))
			.toList();
	}

	private PharmacyResponse withWalkingRoute(Candidate c, double originLat, double originLng) {
		WalkingRoute route = walkingRouteService.routeOrEstimate(
			originLat, originLng, c.latitude(), c.longitude(), c.name());
		return new PharmacyResponse(
			c.id(), c.type(), c.name(), c.address(), c.phone(), c.latitude(), c.longitude(),
			c.distanceMeters(), c.openNow(), c.todayHours(),
			route.totalTimeSeconds(), route.totalDistanceMeters(), route.path()
		);
	}

	/** 오늘 마감이 21시를 넘기거나 자정을 넘어가면 "야간" 레이어에 포함한다. */
	private boolean opensLateEnough(Pharmacy p, DayOfWeek day) {
		LocalTime open = openTimeFor(p, day);
		LocalTime close = closeTimeFor(p, day);
		if (open == null || close == null) {
			return false;
		}
		return close.isAfter(NIGHT_THRESHOLD) || close.isBefore(open);
	}

	// 공휴일(DUTYTIME8) 컬럼은 아직 별도 공휴일 캘린더 연동 전이라 이번 계산에는 반영하지
	// 않는다(요일별 컬럼만 사용) - 명절/공휴일 특수 영업시간은 후속 작업.
	private Candidate toGeneralCandidate(Pharmacy p, double lat, double lng, ZonedDateTime now) {
		double distance = GeoUtils.haversineMeters(lat, lng, p.getLatitude(), p.getLongitude());
		DayOfWeek day = now.getDayOfWeek();
		LocalTime open = openTimeFor(p, day);
		LocalTime close = closeTimeFor(p, day);
		boolean openNow = isOpen(open, close, now.toLocalTime());
		String hours = (open == null || close == null) ? "오늘 휴무" : "%s~%s".formatted(open, close);
		return new Candidate(
			"G-" + p.getId(), PharmacyResponse.TYPE_GENERAL, p.getName(), p.getAddress(), p.getPhone(),
			p.getLatitude(), p.getLongitude(), distance, openNow, hours
		);
	}

	// 22:00~01:00처럼 자정을 넘기는 심야 운영 특성상, "오늘 요일이 운영일이고 22시 이후"이거나
	// "어제 요일이 운영일이고 아직 01시 전"이면 지금 열려있는 것으로 판단한다.
	private Candidate toNightCandidate(NightDutyPharmacy p, double lat, double lng, ZonedDateTime now) {
		double distance = GeoUtils.haversineMeters(lat, lng, p.getLatitude(), p.getLongitude());
		DayOfWeek today = now.getDayOfWeek();
		DayOfWeek yesterday = today.minus(1);
		LocalTime nowTime = now.toLocalTime();
		boolean operatesToday = operatesOn(p, today);
		boolean operatesYesterday = operatesOn(p, yesterday);
		boolean openNow = (operatesToday && !nowTime.isBefore(p.getNightStart()))
			|| (operatesYesterday && nowTime.isBefore(p.getNightEnd()));
		String hours = operatesToday ? "%s~%s".formatted(p.getNightStart(), p.getNightEnd()) : "오늘 미운영";
		return new Candidate(
			"N-" + p.getId(), PharmacyResponse.TYPE_NIGHT, p.getName(), p.getAddress(), p.getPhone(),
			p.getLatitude(), p.getLongitude(), distance, openNow, hours
		);
	}

	private LocalTime openTimeFor(Pharmacy p, DayOfWeek day) {
		return switch (day) {
			case MONDAY -> p.getMonOpen();
			case TUESDAY -> p.getTueOpen();
			case WEDNESDAY -> p.getWedOpen();
			case THURSDAY -> p.getThuOpen();
			case FRIDAY -> p.getFriOpen();
			case SATURDAY -> p.getSatOpen();
			case SUNDAY -> p.getSunOpen();
		};
	}

	private LocalTime closeTimeFor(Pharmacy p, DayOfWeek day) {
		return switch (day) {
			case MONDAY -> p.getMonClose();
			case TUESDAY -> p.getTueClose();
			case WEDNESDAY -> p.getWedClose();
			case THURSDAY -> p.getThuClose();
			case FRIDAY -> p.getFriClose();
			case SATURDAY -> p.getSatClose();
			case SUNDAY -> p.getSunClose();
		};
	}

	private boolean operatesOn(NightDutyPharmacy p, DayOfWeek day) {
		return switch (day) {
			case MONDAY -> p.isOperatesMon();
			case TUESDAY -> p.isOperatesTue();
			case WEDNESDAY -> p.isOperatesWed();
			case THURSDAY -> p.isOperatesThu();
			case FRIDAY -> p.isOperatesFri();
			case SATURDAY -> p.isOperatesSat();
			case SUNDAY -> p.isOperatesSun();
		};
	}

	private boolean isOpen(LocalTime open, LocalTime close, LocalTime now) {
		if (open == null || close == null) {
			return false;
		}
		if (close.isAfter(open)) {
			return !now.isBefore(open) && now.isBefore(close);
		}
		// 마감 시각이 자정을 넘겨 다음날로 기록된 경우(예: 마감 02:00)
		return !now.isBefore(open) || now.isBefore(close);
	}
}
