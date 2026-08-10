package com.chamjima.backend.domain;

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

@Entity
@Table(name = "restrooms", uniqueConstraints = @UniqueConstraint(columnNames = "external_id"))
@Getter
@Setter
@NoArgsConstructor
public class Restroom {

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

	private String category;

	@Column(nullable = false)
	private String address;

	private Double latitude;

	private Double longitude;

	@Column(name = "open_hours")
	private String openHours;

	@Column(name = "has_diaper_table")
	private boolean hasDiaperTable;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status = Status.ACTIVE;

	/**
	 * 같은 화장실을 가리키는 대표 행의 내부 id. null이면 이 행이 대표(=지도/목록에 노출).
	 *
	 * <p>공공데이터 원본이 동일 시설을 여러 관리번호로 중복 수록하기 때문에 필요하다
	 * (2026-08 기준 5,619행 중 700행이 중복). external_id는 원본 추적용이라 중복을 막지
	 * 못하므로, 삭제 대신 대표 행을 가리키게 해서 원본 행과 거기 달린 리뷰를 모두 보존한다.
	 * 대표는 그룹에서 가장 작은 내부 id로 고정한다 — 재계산해도 바뀌지 않아야 리뷰가
	 * 딸려 움직이지 않는다.
	 */
	@Column(name = "canonical_id")
	private Long canonicalId;
}
