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
 * 서울시 약국 운영시간 정보(서울 열린데이터광장) 기반 "전체" 약국 레이어.
 * 요일별 등록 영업시간으로 "지금 영업 중"을 서버에서 계산한다.
 */
@Entity
@Table(name = "pharmacies", uniqueConstraints = @UniqueConstraint(columnNames = "external_id"))
@Getter
@Setter
@NoArgsConstructor
public class Pharmacy {

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

	@Column(name = "mon_open")
	private LocalTime monOpen;
	@Column(name = "mon_close")
	private LocalTime monClose;

	@Column(name = "tue_open")
	private LocalTime tueOpen;
	@Column(name = "tue_close")
	private LocalTime tueClose;

	@Column(name = "wed_open")
	private LocalTime wedOpen;
	@Column(name = "wed_close")
	private LocalTime wedClose;

	@Column(name = "thu_open")
	private LocalTime thuOpen;
	@Column(name = "thu_close")
	private LocalTime thuClose;

	@Column(name = "fri_open")
	private LocalTime friOpen;
	@Column(name = "fri_close")
	private LocalTime friClose;

	@Column(name = "sat_open")
	private LocalTime satOpen;
	@Column(name = "sat_close")
	private LocalTime satClose;

	@Column(name = "sun_open")
	private LocalTime sunOpen;
	@Column(name = "sun_close")
	private LocalTime sunClose;

	@Column(name = "holiday_open")
	private LocalTime holidayOpen;
	@Column(name = "holiday_close")
	private LocalTime holidayClose;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status = Status.ACTIVE;
}
