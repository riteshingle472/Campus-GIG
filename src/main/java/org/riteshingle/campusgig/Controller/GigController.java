package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.*;
import org.riteshingle.campusgig.ResponseDTO.JobApplicationSortingAndFilteringResponseDTO;
import org.riteshingle.campusgig.Service.GigService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gig")
@RequiredArgsConstructor
public class GigController {
    private final GigService gigService;

//    Become a GIG
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/become-gig")
    public ResponseEntity<?> becomeGig(@RequestBody BecomeGigRequestDTO dto){
        gigService.becomeGig(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

//    Add Skills ( ) -> GIG
    @PreAuthorize("hasRole('GIG')")
    @PatchMapping("/add-skills")
    public ResponseEntity<?> addSkills(@RequestBody AddSkillsRequestDTO dto){
        gigService.addSkills(dto.getSkillsId());
        return ResponseEntity.noContent().build();
    }

//    Job Proposal ( ) -> GIG
    @PreAuthorize("hasRole('GIG')")
    @PostMapping("/proposal")
    public ResponseEntity<?> applyForJob(@RequestBody JobApplicationRequestDTO dto){
        gigService.applyForJob(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

//    Update Proposal ( ) -> GIG
    @PreAuthorize("hasRole('GIG')")
    @PatchMapping("/proposal/{jobApplicationId}")
    public ResponseEntity<?> updateJobApplication(@PathVariable Long jobApplicationId , @RequestBody UpdateJobApplicationRequestDTO dto){
        gigService.updateJobApplication(jobApplicationId,dto);
        return ResponseEntity.noContent().build();
    }

//    Withdrawn Proposal ( ) -> GIG
    @PreAuthorize("hasRole('GIG')")
    @PatchMapping("/withdraw-proposal/{jobApplicationId}")
    public ResponseEntity<?> withdrawJobByJobApplicationId(@PathVariable Long jobApplicationId){
        gigService.withdrawJobApplicationByJobApplicationId(jobApplicationId);
        return ResponseEntity.noContent().build();
    }

//    All Proposals ( ) -> GIG
    @PreAuthorize("hasRole('GIG')")
    @GetMapping("/proposals ")
    public ResponseEntity<List<JobApplicationSortingAndFilteringResponseDTO>> getAllJobApplication(@RequestBody JobApplicationFilterAndSortingRequestDTO dto,
                                                                                                   @RequestParam(required = false,defaultValue = "10") int pageSize,
                                                                                                   @RequestParam(required = false,defaultValue = "1") int pageNumber){
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        return ResponseEntity.ok(gigService.getAllJobApplication(dto,pageable));
    }
}
