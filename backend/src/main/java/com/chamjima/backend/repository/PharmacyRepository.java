package com.chamjima.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.chamjima.backend.domain.Pharmacy;

public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {
	Optional<Pharmacy> findByExternalId(String externalId);

	List<Pharmacy> findByStatusAndLatitudeIsNotNull(Pharmacy.Status status);
}
