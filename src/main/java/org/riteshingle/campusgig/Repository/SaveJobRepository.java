package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.SaveJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaveJobRepository extends JpaRepository<SaveJob,Long> {
}
