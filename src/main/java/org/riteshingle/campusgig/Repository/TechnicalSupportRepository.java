package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.TechnicalSupport;
import org.riteshingle.campusgig.Model.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicalSupportRepository extends JpaRepository<TechnicalSupport,Long>{
    @Query("Select ts from TechnicalSupport ts where reporter = :reporter ")
    List<TechnicalSupport> findMyTechnicalSupportReport(@Param("reporter") UserEntity currentProfile, Pageable pageable);

    Optional<TechnicalSupport> findByIdAndReporter(Long id, UserEntity currentProfile);

    @Query("Select COUNT(ts) from TechnicalSupport ts where ts.createdAt >= :from AND ts.createdAt <= :to")
    Long findTotalTechnicalIssues(@Param("from") LocalDateTime startFrom,@Param("to") LocalDateTime endTo);
}
