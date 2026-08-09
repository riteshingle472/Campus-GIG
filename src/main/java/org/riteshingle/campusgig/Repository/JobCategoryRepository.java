package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.Optional;

@Repository
public interface JobCategoryRepository extends JpaRepository<JobCategory,Long> {
    Optional<JobCategory> findByCategory(String category);

    Boolean existsByCategory(String category);
}
