package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.JobRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.JobApplicantResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.JobResponseDTO;
import org.riteshingle.campusgig.Service.GigService;
import org.riteshingle.campusgig.Service.JobService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/job")
public class JobController {
    private final JobService jobService;
    private final GigService gigService;

//    For -> Client
//    Create Job
    @PostMapping("/create-job")
    @PreAuthorize("/hasRole('USER')")
    public ResponseEntity<String> createJob(@RequestBody JobRequestDTO dto){
        return ResponseEntity.ok(jobService.publishJob(dto));
    }

//    For -> EveryOne
//    Get all jobs
    @PreAuthorize("hasRole('USER','GIG'")
    @GetMapping("/jobs")
    public ResponseEntity<List<JobResponseDTO>> getJobs(@RequestParam(defaultValue = "1",required = false) int pageNumber,
                                                        @RequestParam(defaultValue = "10",required = false) int pageSize,
                                                        @RequestParam(defaultValue = "budget",required = false) String byField,
                                                        @RequestParam(defaultValue = "ASC",required = false)String direction){
        Pageable pageable = PageRequest.of(pageNumber-1,pageSize, Sort.Direction.fromString(direction),byField);
        return ResponseEntity.ok(jobService.getJobs(pageable));
    }

//    For -> Everyone
//    Get Job by ID
    @PreAuthorize("hasRole('USER','GIG'")
    @GetMapping("/get-job/{id}")
    public ResponseEntity<JobResponseDTO> getJob(@PathVariable Long id){
        return ResponseEntity.ok(jobService.getJob(id));
    }

//    For -> client
//    Draft Job
    @PreAuthorize("/hasRole('USER')")
    @PostMapping("/draft-job")
    public ResponseEntity<String> draftJob(@RequestBody JobRequestDTO dto){
        return ResponseEntity.ok(jobService.draftJob(dto));
    }

//    For -> client
//    Get Draft Job
    @PreAuthorize("/hasRole('USER')")
    @GetMapping("/draft-job/{draftId}")
    public ResponseEntity<JobRequestDTO> getDraft(@PathVariable String draftId){
        return ResponseEntity.ok(jobService.getDraft(draftId));
    }

//    For -> client
//    Delete Job
//    Soft delete
    @PreAuthorize("/hasRole('USER')")
    @DeleteMapping("/delete-job")
    public ResponseEntity<String> deleteJob(@RequestParam Long jobId){
         return ResponseEntity.ok(jobService.deleteJob(jobId));
    }

//    For -> client
//    Remove Draft
//    Permanent delete
    @PreAuthorize("/hasRole('USER')")
    @DeleteMapping("/remove-draft/{draftId}")
    public ResponseEntity<?> removeDraft(@PathVariable String draftId){
        jobService.removeDraft(draftId);
        return ResponseEntity.noContent().build();
    }

//    For -> Client
//    Get all draft Job
    @PreAuthorize("/hasRole('USER')")
    @GetMapping("/draft-jobs")
    public ResponseEntity<List<JobRequestDTO>> getAllDraftJob(){
        return ResponseEntity.of(Optional.ofNullable(jobService.getAllDraft()));
    }

//    For -> Client
//    Edit published job
    @PreAuthorize("/hasRole('USER')")
    @PutMapping("/edit-job/{id}")
    public ResponseEntity<String> editJob(@PathVariable Long id, @RequestBody JobRequestDTO dto){
        return ResponseEntity.ok(jobService.editJob(dto,id));
    }

//    For -> Client
//    Edit published job
    @PreAuthorize("/hasRole('USER')")
    @PutMapping("/update-draft-job")
    public ResponseEntity<String> updateJob(@RequestBody JobRequestDTO dto){
        return ResponseEntity.ok(jobService.updateDraftJob(dto));
    }

    @PreAuthorize("/hasRole('USER')")
    @GetMapping("/job-applicants/{jobId}")
    public ResponseEntity<List<JobApplicantResponseDTO>> getAllJobApplicant(@PathVariable Long jobId){
        return ResponseEntity.ok(jobService.getAllJobApplicant(jobId));
    }

    @PreAuthorize("/hasRole('USER')")
    @GetMapping("/client-posted-jobs")
    public ResponseEntity<List<JobResponseDTO>> getJobsPostByMe(@RequestParam String status){
        return ResponseEntity.ok(jobService.getAllJobsPostByMe(status));
    }

    @PreAuthorize("/hasRole('GIG')")
    @PostMapping("/withdraw-job-application/{jobId}")
    public ResponseEntity<String> withdrawJobApplicationByJobId(@PathVariable Long jobId){
        return ResponseEntity.ok(gigService.withdrawJobApplicationByJobId(jobId));
    }

    @PreAuthorize("/hasRole('USER')")
    @PostMapping("/accept-job-proposal")
    public ResponseEntity<?> acceptJobApplication(@RequestParam Long jobId,@RequestParam Long applicationId){
        jobService.acceptJobProposal(jobId,applicationId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("/hasRole('USER')")
    @PostMapping("/reject-job-proposal")
    public ResponseEntity<?> rejectJobApplication(@RequestParam Long applicationId){
        jobService.rejectJobProposal(applicationId);
        return ResponseEntity.noContent().build();
    }
}