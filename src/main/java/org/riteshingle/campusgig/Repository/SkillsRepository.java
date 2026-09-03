package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.Skills;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillsRepository extends JpaRepository<Skills, Long> {
    Boolean existsBySkill(String skill);

    @Query("""
    SELECT s
    FROM Skills s
    WHERE :keyword IS NULL
       OR :keyword = ''
       OR LOWER(s.skill) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<Skills> searchSkills(@Param("keyword") String keyword,Pageable pageable);
}
