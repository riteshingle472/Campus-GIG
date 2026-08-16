package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Enum.JobStatus;
import org.riteshingle.campusgig.Model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface JobRepository extends JpaRepository<Job,Long>, JpaSpecificationExecutor<Job> {

    @Query("SELECT j FROM Job j where j.user.id = :clientId and j.jobStatus = :status")
    List<Job> findJobsByClientIdAndStatus(@Param("clientId") Long id,@Param("status") JobStatus status);
}
