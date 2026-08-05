package org.riteshingle.campusgig.Repository;

import org.riteshingle.campusgig.Model.Skills;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillsRepository extends JpaRepository<Skills, Long> {
    Boolean existsBySkill(String skill);
}
