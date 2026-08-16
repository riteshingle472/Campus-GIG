package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.*;
import org.riteshingle.campusgig.ResponseDTO.JobApplicationSortingAndFilteringResponseDTO;
import org.riteshingle.campusgig.Service.GigService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gig")
@RequiredArgsConstructor
public class GigController {
    private final GigService gigService;

    @PostMapping("/become-gig")
    public ResponseEntity<String> becomeGig(@RequestBody BecomeGigRequestDTO dto){
        return ResponseEntity.ok(gigService.becomeGig(dto));
    }

    @PreAuthorize("/hasRole('GIG')")
    @PostMapping("/add-skills")
    public ResponseEntity<String> addSkills(@RequestBody AddSkillsRequestDTO dto){
        return ResponseEntity.ok(gigService.addSkills(dto.getSkillsId()));
    }

    @PreAuthorize("/hasRole('GIG')")
    @PostMapping("/apply-job")
    public ResponseEntity<String> applyForJob(@RequestBody JobApplicationRequestDTO dto){
        return ResponseEntity.ok(gigService.applyForJob(dto));
    }

    @PreAuthorize("/hasRole('GIG')")
    @PutMapping("/update-job-application/{jobApplicationId}")
    public ResponseEntity<String> updateJobApplication(@PathVariable Long jobApplicationId , @RequestBody UpdateJobApplicationRequestDTO dto){
        return ResponseEntity.ok(gigService.updateJobApplication(jobApplicationId,dto));
    }

    @PreAuthorize("/hasRole('GIG')")
    @PostMapping("/withdraw-job-application/{jobApplicationId}")
    public ResponseEntity<String> withdrawJobByJobApplicationId(@PathVariable Long jobApplicationId){
        return ResponseEntity.ok(gigService.withdrawJobApplicationByJobApplicationId(jobApplicationId));
    }

    @PreAuthorize("/hasRole('GIG')")
    @GetMapping("/get-all-job-application")
    public ResponseEntity<List<JobApplicationSortingAndFilteringResponseDTO>> getAllJobApplication(@RequestBody JobApplicationFilterAndSortingRequestDTO dto,
                                                                                                   @RequestParam(required = false,defaultValue = "10") int pageSize,
                                                                                                   @RequestParam(required = false,defaultValue = "1") int pageNumber){
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        return ResponseEntity.ok(gigService.getAllJobApplication(dto,pageable));
    }
}
