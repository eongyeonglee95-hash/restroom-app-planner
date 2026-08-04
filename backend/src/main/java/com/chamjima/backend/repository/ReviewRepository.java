package com.chamjima.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.chamjima.backend.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
	List<Review> findByRestroomIdOrderByCreatedAtDesc(Long restroomId);

	List<Review> findByRestroomIdIn(List<Long> restroomIds);
}
