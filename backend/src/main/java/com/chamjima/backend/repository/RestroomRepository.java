package com.chamjima.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.chamjima.backend.domain.Restroom;

public interface RestroomRepository extends JpaRepository<Restroom, Long> {
	Optional<Restroom> findByExternalId(String externalId);

	List<Restroom> findByStatusAndLatitudeIsNotNull(Restroom.Status status);
}
