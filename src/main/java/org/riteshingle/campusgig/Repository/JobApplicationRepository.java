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

    @Query("SELECT COUNT(j) FROM JobApplication j WHERE j.createAt >= :fromDate AND j.createAt <= :toDate AND j.jobApplicationStatus = :status")
    Long findTotalJobApplicationByStatus(@Param("status") JobApplicationStatus jobApplicationStatus,@Param("fromDate") LocalDate from,@Param("toDate") LocalDate to);

    @Query("""
                SELECT FUNCTION('DATE', a.createAt), COUNT(a)
                FROM JobApplication a
                WHERE a.jobApplicationStatus = :status AND a.createAt >= :fromDate
                AND a.createAt <= :toDate
                GROUP BY FUNCTION('DATE', a.createAt)
                ORDER BY FUNCTION('DATE', a.createAt)
            """)
    List<Object[]> getApplicationGrowth(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") JobApplicationStatus jobApplicationStatus
    );
}
