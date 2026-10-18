package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.JobRequestDTO;
import org.riteshingle.campusgig.RequestDTO.JobsSpecificationRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.JobApplicantResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.JobResponseDTO;
import org.riteshingle.campusgig.Service.GigService;
import org.riteshingle.campusgig.Service.JobService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/job")
public class JobController {
    private final JobService jobService;
    private final GigService gigService;

//    Post Job
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/job")
    public ResponseEntity<JobResponseDTO> createJob(@RequestBody JobRequestDTO dto) {
        return ResponseEntity.ok(jobService.publishJob(dto));
    }

//    Jobs
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG') or hasRole('USER')")
    @GetMapping("/jobs")
    public ResponseEntity<List<JobResponseDTO>> getJobs(@RequestParam(defaultValue = "1", required = false) int pageNumber,
                                                        @RequestParam(defaultValue = "10", required = false) int pageSize,
                                                        @RequestParam(defaultValue = "budget", required = false) String byField,
                                                        @RequestParam(defaultValue = "ASC", required = false) String direction,
                                                        @RequestParam(required = false) String search,
                                                        @RequestBody(required = false) JobsSpecificationRequestDTO jobsSpecificationRequestDTO) {
        return ResponseEntity.ok(jobService.getJobs(pageNumber,pageSize,direction,byField,search ,jobsSpecificationRequestDTO));
    }

//    Get Job
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @GetMapping("/job/{id}")
    public ResponseEntity<JobResponseDTO> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJob(id));
    }

//    Draft
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/draft")
    public ResponseEntity<?> draftJob(@RequestBody JobRequestDTO dto) {
        jobService.draftJob(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

//    Get Draft
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/draft/{draftId}")
    public ResponseEntity<JobRequestDTO> getDraft(@PathVariable String draftId) {
        return ResponseEntity.ok(jobService.getDraft(draftId));
    }

//    Delete Job
    @PreAuthorize("hasRole('CLIENT')")
    @DeleteMapping("/job")
    public ResponseEntity<?> deleteJob(@RequestParam Long jobId) {
        jobService.deleteJob(jobId);
        return ResponseEntity.noContent().build();
    }

//    Delete Draft
    @PreAuthorize("hasRole('CLIENT')")
    @DeleteMapping("/draft/{draftId}")
    public ResponseEntity<?> removeDraft(@PathVariable String draftId) {
        jobService.removeDraft(draftId);
        return ResponseEntity.noContent().build();
    }

//    Drafts
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/drafts")
    public ResponseEntity<List<JobRequestDTO>> getAllDraftJob() {
        return ResponseEntity.of(Optional.ofNullable(jobService.getAllDraft()));
    }

//    Edit Job
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/job/{id}")
    public ResponseEntity<JobResponseDTO> editJob(@PathVariable Long id, @RequestBody JobRequestDTO dto) {
        return ResponseEntity.ok(jobService.editJob(dto, id));
    }

//    Edit Draft
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/draft")
    public ResponseEntity<?> updateJob(@RequestBody JobRequestDTO dto) {
        jobService.updateDraftJob(dto);
        return ResponseEntity.noContent().build();
    }

//    Job Applicants
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/applicants/{jobId}")
    public ResponseEntity<List<JobApplicantResponseDTO>> getAllJobApplicants(@PathVariable Long jobId,
                                                                             @RequestParam(required = false, defaultValue = "1") int page,
                                                                             @RequestParam(required = false, defaultValue = "10") int size,
                                                                             @RequestParam(required = false,defaultValue = "ASC")String direction,
                                                                             @RequestParam(required = false,defaultValue = "createdAt")String field,
                                                                             @RequestParam(required = false,defaultValue = "APPLIED")String keyword) {
        return ResponseEntity.ok(jobService.getAllJobApplicants(jobId, page,size,direction,field,keyword));
    }

//    My Posted Job
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/my-jobs")
    public ResponseEntity<List<JobResponseDTO>> getJobsPostByMe(@RequestParam(required = false,defaultValue = "OPEN") String status,
                                                                @RequestParam(required = false, defaultValue = "1") int page,
                                                                @RequestParam(required = false, defaultValue = "10") int size,
                                                                @RequestParam(required = false, defaultValue = "ASC") String direction,
                                                                @RequestParam(required = false, defaultValue = "publishAt") String field){
        return ResponseEntity.ok(jobService.getAllJobsPostByMe(status, page,size,direction,field));
    }

//    Withdrawn Job Proposal By Job ID
    @PreAuthorize("hasRole('GIG')")
    @PatchMapping("/withdraw-proposal/{jobId}")
    public ResponseEntity<?> withdrawJobApplicationByJobId(@PathVariable Long jobId) {
        gigService.withdrawJobApplicationByJobId(jobId);
        return ResponseEntity.noContent().build();
    }

//    Accept Proposal
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/accept-proposal")
    public ResponseEntity<?> acceptJobApplication(@RequestParam Long jobId, @RequestParam Long applicationId) {
        jobService.acceptJobProposal(jobId, applicationId);
        return ResponseEntity.noContent().build();
    }

//    Reject Proposal
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/reject-proposal")
    public ResponseEntity<?> rejectJobApplication(@RequestParam Long applicationId) {
        jobService.rejectJobProposal(applicationId);
        return ResponseEntity.noContent().build();
    }

//    Shortlist a proposal
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/shortlist-proposal")
    public ResponseEntity<?> shortlistJobApplication(@RequestParam Long applicationId) {
        jobService.shortlistJobProposal(applicationId);
        return ResponseEntity.noContent().build();
    }
}