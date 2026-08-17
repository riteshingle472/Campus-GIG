package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaveJobRepository extends JpaRepository<Bookmark,Long> {
    boolean existsByGigIdAndJobId(Long id, Long jobId);

    Optional<Bookmark> findByJobIdAndGigId(Long jobId, Long id);

    List<Bookmark> findByGigId(Long id);
}
