package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.ResponseDTO.SkillResponseDTO;
import org.riteshingle.campusgig.Service.SkillsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/skills")
public class SkillsController {
    private final SkillsService skillsService;

    @GetMapping("/skills")
    public ResponseEntity<List<SkillResponseDTO>> getSkills(){
        return ResponseEntity.ok(skillsService.getSkills());
    }

    @GetMapping("/skill/{id}")
    public ResponseEntity<SkillResponseDTO> getSkill(@PathVariable Long id){
        return ResponseEntity.ok(skillsService.getSkill(id));
    }

    @PostMapping("/add-skill")
    public ResponseEntity<?> addSkill(@RequestParam String skill){
        skillsService.addSkill(skill);
        return ResponseEntity.noContent().build();
    }
}
