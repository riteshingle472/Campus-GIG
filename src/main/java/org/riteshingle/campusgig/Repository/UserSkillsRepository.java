package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.UserSkills;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSkillsRepository extends JpaRepository<UserSkills ,Long> {
    @Query("SELECT us.skill.id FROM UserSkills us WHERE us.gig.id = :gigId")
    List<Long> findSkillIdsByGigId(@Param("gigId") Long gigId);

    @Query("SELECT us.skill.skill FROM UserSkills us WHERE us.gig.id = :gigId")
    List<String> findSkillByGigId(@Param("gigId") Long gigId);
}
