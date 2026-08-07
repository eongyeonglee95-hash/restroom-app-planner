package com.chamjima.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.chamjima.backend.domain.NightDutyPharmacy;

public interface NightDutyPharmacyRepository extends JpaRepository<NightDutyPharmacy, Long> {
	Optional<NightDutyPharmacy> findByExternalId(String externalId);

	List<NightDutyPharmacy> findByStatusAndLatitudeIsNotNull(NightDutyPharmacy.Status status);
}
