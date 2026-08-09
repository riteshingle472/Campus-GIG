package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.ResponseDTO.SkillResponseDTO;
import org.riteshingle.campusgig.Service.SkillsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/skills")
public class SkillsController {
    private final SkillsService skillsService;

//    Get All skills
    @GetMapping("/skills")
    public ResponseEntity<List<SkillResponseDTO>> getSkills(@RequestParam(defaultValue = "1",required = false) int pageNumber,
                                                            @RequestParam(defaultValue = "10",required = false) int pageSize,
                                                            @RequestParam(defaultValue = "skill",required = false) String sortBy,
                                                            @RequestParam(defaultValue = "ASC",required = false) String sortDirection){

        Pageable pageable = PageRequest.of(pageNumber-1, pageSize,Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        return ResponseEntity.ok(skillsService.getSkills(pageable));
    }

//    Get Skill by ID
    @GetMapping("/skill/{id}")
    public ResponseEntity<SkillResponseDTO> getSkill(@PathVariable Long id){
        return ResponseEntity.ok(skillsService.getSkill(id));
    }

//    Add skill ADMIN Only
    @PostMapping("/add-skill")
    public ResponseEntity<?> addSkill(@RequestParam String skill){
        skillsService.addSkill(skill);
        return ResponseEntity.noContent().build();
    }
}
