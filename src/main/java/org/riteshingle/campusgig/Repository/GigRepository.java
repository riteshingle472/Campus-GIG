package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.GIG;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GigRepository extends JpaRepository<GIG,Long> {
}
