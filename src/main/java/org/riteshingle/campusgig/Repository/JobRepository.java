package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Enum.JobStatus;
import org.riteshingle.campusgig.Model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SearchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    @Query("SELECT j FROM Job j where j.client.id = :clientId and (:status IS NULL OR j.jobStatus = :status)")
    List<Job> findJobsByClientIdAndStatus(@Param("clientId") Long id, @Param("status") JobStatus status,Pageable pageable);

    @Query("SELECT COUNT(j) FROM Job j WHERE  j.publishAt >= :fromDate AND j.publishAt <= :toDate AND (:status Is NULL OR j.jobStatus = :status)")
    Long findTotalJobByStatus(@Param("status") JobStatus jobStatus,@Param("fromDate")LocalDateTime from ,@Param("toDate")LocalDateTime to);

    @Query("""
                SELECT j.category, COUNT(j) FROM Job j
                WHERE j.publishAt BETWEEN :fromDate AND :toDate
                GROUP BY j.category
                ORDER BY COUNT(j) DESC
            """)
    List<Object[]> findPopularJobCategories(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
                SELECT FUNCTION('DATE', j.publishAt), COUNT(j)
                FROM Job j
                WHERE j.publishAt >= :fromDate
                AND j.publishAt < :toDate
                GROUP BY FUNCTION('DATE', j.publishAt)
                ORDER BY FUNCTION('DATE', j.publishAt)
            """)
    List<Object[]> getJobGrowth(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    Page<Job> findByJobStatus(JobStatus jobStatus, Pageable pageable);

}
