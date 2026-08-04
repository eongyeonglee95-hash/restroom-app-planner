package com.chamjima.backend.domain;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class Review {

	public enum Mood {
		GOOD, NORMAL, BAD
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "restroom_id", nullable = false)
	private Long restroomId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Mood mood;

	@Column(name = "has_tissue")
	private boolean hasTissue;

	@Column(name = "has_bidet")
	private boolean hasBidet;

	@Column(name = "no_line")
	private boolean noLine;

	@Column(name = "is_free")
	private boolean isFree;

	@Column(name = "is_clean")
	private boolean isClean;

	@Column(name = "no_passcode")
	private boolean noPasscode;

	@Column(length = 200)
	private String comment;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
