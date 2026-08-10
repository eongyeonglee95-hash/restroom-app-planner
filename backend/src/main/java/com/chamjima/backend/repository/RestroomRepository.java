package com.chamjima.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.chamjima.backend.domain.Restroom;

public interface RestroomRepository extends JpaRepository<Restroom, Long> {
	Optional<Restroom> findByExternalId(String externalId);

	/** 지도/목록 노출용. 중복 행(canonical_id != null)은 제외한다. */
	List<Restroom> findByStatusAndCanonicalIdIsNullAndLatitudeIsNotNull(Restroom.Status status);
}
