package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.processing.Find;
import org.riteshingle.campusgig.Model.Skills;
import org.riteshingle.campusgig.Repository.SkillsRepository;
import org.riteshingle.campusgig.ResponseDTO.SkillResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillsService {
    private final SkillsRepository skillsRepository;

//    Get Skills
    public List<SkillResponseDTO> getSkills(Pageable pageable){
//        fetching skills
        List<Skills> skills = skillsRepository.findAll(pageable).getContent();
//        return in response DTO List
        return skills.stream().map(this::skillResponseDTO).toList();
    }

//    Skill by ID
    public SkillResponseDTO getSkill(Long id){
//        Find skill by ID and return in skill response
        Skills skills = skillsRepository.findById(id).orElseThrow(() -> new RuntimeException("Skill not found with id : " + id));
        return skillResponseDTO(skills);
    }

//    ADMIN Only () ->
//    Add Skill
    public void addSkill(String skill){
//        Check Skill is already exists ?
        Boolean exists = skillsRepository.existsBySkill(skill);
        if(exists) throw new RuntimeException("skill already Exist");
//        Save in DB after capatalize first character
        skill = skill.substring(0, 1).toUpperCase() + skill.substring(1);
        Skills skills = Skills.builder().skill(skill).build();
        skillsRepository.save(skills);
    }
//   helper methods

//    Skill response DTO () ->
    private SkillResponseDTO skillResponseDTO(Skills skills){
        return SkillResponseDTO.builder()
                .skill(skills.getSkill())
                .id(skills.getId())
                .build();
    }
}
