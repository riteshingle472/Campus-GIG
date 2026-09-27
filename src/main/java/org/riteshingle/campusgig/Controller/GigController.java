package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.*;
import org.riteshingle.campusgig.ResponseDTO.GigResponseDTO;
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

//    Add Skills
    @PreAuthorize("hasRole('GIG')")
    @PatchMapping("/add-skills")
    public ResponseEntity<?> addSkills(@RequestBody AddSkillsRequestDTO dto){
        gigService.addSkills(dto.getSkillsId());
        return ResponseEntity.noContent().build();
    }

//    Proposal
    @PreAuthorize("hasRole('GIG')")
    @PostMapping("/proposal")
    public ResponseEntity<?> applyForJob(@RequestBody JobApplicationRequestDTO dto){
        gigService.applyForJob(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

//    Update Proposal
    @PreAuthorize("hasRole('GIG')")
    @PatchMapping("/proposal/{jobApplicationId}")
    public ResponseEntity<?> updateJobApplication(@PathVariable Long jobApplicationId , @RequestBody UpdateJobApplicationRequestDTO dto){
        gigService.updateJobApplication(jobApplicationId,dto);
        return ResponseEntity.noContent().build();
    }

//    Withdrawn Proposal by Job Application id
    @PreAuthorize("hasRole('GIG')")
    @PatchMapping("/withdraw-proposal/{jobApplicationId}")
    public ResponseEntity<?> withdrawJobByJobApplicationId(@PathVariable Long jobApplicationId){
        gigService.withdrawJobApplicationByJobApplicationId(jobApplicationId);
        return ResponseEntity.noContent().build();
    }

//    Get All GIG Proposals
    @PreAuthorize("hasRole('GIG')")
    @PostMapping("/proposals")
    public ResponseEntity<List<JobApplicationSortingAndFilteringResponseDTO>> getAllJobApplication(
            @RequestBody JobApplicationFilterAndSortingRequestDTO dto,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "1") int pageNumber){
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        return ResponseEntity.ok(gigService.getAllJobApplication(dto, pageable));
    }

//    GIG Profile
    @PreAuthorize("hasRole('GIG')")
    @GetMapping("/profile")
    public ResponseEntity<GigResponseDTO> getMyGigProfile() {
        return ResponseEntity.ok(gigService.getMyGigProfile());
    }

//    edit GIG Profile
    @PreAuthorize("hasRole('GIG')")
    @PatchMapping("/profile")
    public ResponseEntity<?> editGigProfile(@RequestBody BecomeGigRequestDTO dto) {
        gigService.editGigProfile(dto);
        return ResponseEntity.noContent().build();

    }

//    Get GIG Profile by ID
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @GetMapping("/{gigId}")
    public ResponseEntity<GigResponseDTO> getGigProfileById(@PathVariable Long gigId) {
        return ResponseEntity.ok(gigService.getGigProfileById(gigId));
    }
}