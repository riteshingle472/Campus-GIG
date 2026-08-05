package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Model.Skills;
import org.riteshingle.campusgig.Repository.SkillsRepository;
import org.riteshingle.campusgig.ResponseDTO.SkillResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillsService {
    private final SkillsRepository skillsRepository;

    public List<SkillResponseDTO> getSkills(){
        List<Skills> skills = skillsRepository.findAll();
        return skills.stream().map(this::skillResponseDTO).toList();
    }

    public SkillResponseDTO getSkill(Long id){
        Skills skills = skillsRepository.findById(id).orElseThrow(() -> new RuntimeException("Skill not found with id : " + id));
        return skillResponseDTO(skills);
    }

    public void addSkill(String skill){
        Boolean exists = skillsRepository.existsBySkill(skill);
        if(exists) throw new RuntimeException("skill already Exist");
        skill = skill.substring(0, 1).toUpperCase() + skill.substring(1);
        Skills skills = Skills.builder().skill(skill).build();
        skillsRepository.save(skills);
    }

    private SkillResponseDTO skillResponseDTO(Skills skills){
        return SkillResponseDTO.builder()
                .skill(skills.getSkill())
                .id(skills.getId())
                .build();
    }
}
