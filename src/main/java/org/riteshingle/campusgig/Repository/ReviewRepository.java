package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review,Long> {
    boolean existsByContractIdAndReviewerId(Long contractId,Long reviewerId);
    Page<Review> findByRevieweeId(Long revieweeId, Pageable pageable);

    Optional<Review> findByContractIdAndReviewerId(Long contractId, Long id);
}
