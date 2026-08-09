package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.CreateJobRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.JobResponseDTO;
import org.riteshingle.campusgig.Service.JobService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/job")
public class JobController {
    private final JobService jobService;

//    For -> Client
//    Create Job
    @PostMapping("/create-job")
    public ResponseEntity<String> createJob(@RequestBody CreateJobRequestDTO dto){
        return ResponseEntity.ok(jobService.publishJob(dto));
    }

//    For -> EveryOne
//    Get all jobs
    @GetMapping("/jobs")
    public ResponseEntity<List<JobResponseDTO>> getJobs(@RequestParam(defaultValue = "1",required = false) int pageNumber,
                                                        @RequestParam(defaultValue = "10",required = false) int pageSize,
                                                        @RequestParam(defaultValue = "budget",required = false) String byField,
                                                        @RequestParam(defaultValue = "ASC",required = false)String direction){
        Pageable pageable = PageRequest.of(pageNumber-1,pageSize, Sort.Direction.fromString(direction),byField);
        return ResponseEntity.ok(jobService.getJobs(pageable));
    }

//    For -> GIG
//    Get Job by ID
    @GetMapping("/get-job/{id}")
    public ResponseEntity<JobResponseDTO> getJob(@PathVariable Long id){
        return ResponseEntity.ok(jobService.getJob(id));
    }

//    For -> client
//    Draft Job
    @PostMapping("/draft-job")
    public ResponseEntity<String> draftJob(@RequestBody CreateJobRequestDTO dto){
        return ResponseEntity.ok(jobService.draftJob(dto));
    }

//    For -> client
//    Get Draft Job
    @GetMapping("/get-draft-job/{draftId}")
    public ResponseEntity<CreateJobRequestDTO> getDraft(@PathVariable String draftId){
        return ResponseEntity.ok(jobService.getDraft(draftId));
    }

//    For -> client
//    Delete Job
//    Soft delete
    @DeleteMapping("/delete-job")
    public ResponseEntity<String> deleteJob(@RequestParam Long id){
         return ResponseEntity.ok(jobService.deleteJob(id));
    }

//    For -> client
//    Remove Draft
//    Permanent delete
    @DeleteMapping("/remove-draft/{draftId}")
    public ResponseEntity<?> removeDraft(@PathVariable String draftId){
        jobService.removeDraft(draftId);
        return ResponseEntity.noContent().build();
    }
}