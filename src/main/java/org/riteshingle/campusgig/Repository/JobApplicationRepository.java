package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Enum.JobApplicationStatus;
import org.riteshingle.campusgig.Model.GIG;
import org.riteshingle.campusgig.Model.Job;
import org.riteshingle.campusgig.Model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {
    Optional<JobApplication> findByGigAndJobAndJobApplicationStatus(GIG gig, Job job, JobApplicationStatus status);

    List<JobApplication> findByJob(Job job);

    Optional<JobApplication> findByJobIdAndGigId(Long jobId, Long gigId);

    boolean existsByJobIdAndGigId(Long jobId, Long gigId);

    @Query("SELECT COUNT(j) FROM JobApplication j WHERE j.createdAt >= :fromDate AND j.createdAt <= :toDate AND j.jobApplicationStatus = :status")
    Long findTotalJobApplicationByStatus(@Param("status") JobApplicationStatus jobApplicationStatus,@Param("fromDate") LocalDateTime from,@Param("toDate") LocalDateTime to);

    @Query("""
                SELECT FUNCTION('DATE', a.createdAt), COUNT(a)
                FROM JobApplication a
                WHERE a.createdAt >= :fromDate
                AND a.createdAt <= :toDate
                GROUP BY FUNCTION('DATE', a.createdAt)
                ORDER BY FUNCTION('DATE', a.createdAt)
            """)
    List<Object[]> getApplicationGrowth(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);
}
