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
}
