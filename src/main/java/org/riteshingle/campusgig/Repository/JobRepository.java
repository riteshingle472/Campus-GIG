package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface JobRepository extends JpaRepository<Job,Long> {
}
