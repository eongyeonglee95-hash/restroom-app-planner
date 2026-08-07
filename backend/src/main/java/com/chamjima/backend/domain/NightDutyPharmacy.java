package com.chamjima.backend.domain;

import java.time.LocalTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 서울시 공공심야약국(25개 자치구 39개소, 2026-08 기준) "야간" 레이어. API가 없어
 * 시 발표 자료를 수동으로 수집해 시드 데이터로 적재한다. "전체"(Pharmacy)와는
 * 매칭하지 않고 별개 소스로 취급(급똥 탭의 검증된 화장실 vs 이용팁과 동일한 원칙).
 * 대부분 매일 밤 22:00~익일 01:00 운영이며, 일부는 특정 요일에만 운영한다.
 */
@Entity
@Table(name = "night_duty_pharmacies", uniqueConstraints = @UniqueConstraint(columnNames = "external_id"))
@Getter
@Setter
@NoArgsConstructor
public class NightDutyPharmacy {

	public enum Status {
		ACTIVE, CLOSED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "external_id", nullable = false)
	private String externalId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String address;

	private Double latitude;

	private Double longitude;

	private String phone;

	@Column(name = "operates_mon")
	private boolean operatesMon;
	@Column(name = "operates_tue")
	private boolean operatesTue;
	@Column(name = "operates_wed")
	private boolean operatesWed;
	@Column(name = "operates_thu")
	private boolean operatesThu;
	@Column(name = "operates_fri")
	private boolean operatesFri;
	@Column(name = "operates_sat")
	private boolean operatesSat;
	@Column(name = "operates_sun")
	private boolean operatesSun;

	@Column(name = "night_start")
	private LocalTime nightStart;

	@Column(name = "night_end")
	private LocalTime nightEnd;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status = Status.ACTIVE;
}
