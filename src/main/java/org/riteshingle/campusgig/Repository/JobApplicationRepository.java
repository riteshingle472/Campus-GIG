package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Enum.JobApplicationStatus;
import org.riteshingle.campusgig.Model.GIG;
import org.riteshingle.campusgig.Model.Job;
import org.riteshingle.campusgig.Model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {
    Optional<JobApplication> findByGigAndJobAndJobApplicationStatus(GIG gig, Job job,JobApplicationStatus status);

    List<JobApplication> findByJob(Job job);

    Optional<JobApplication> findByJobIdAndGigId(Long jobId, Long gigId);
    boolean existsByJobIdAndGigId(Long jobId, Long gigId);
}
