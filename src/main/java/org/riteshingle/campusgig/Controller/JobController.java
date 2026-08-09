package org.riteshingle.campusgig.Controller;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.JobRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.JobResponseDTO;
import org.riteshingle.campusgig.Service.JobService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/job")
public class JobController {
    private final JobService jobService;

//    For -> Client
//    Create Job
    @PostMapping("/create-job")
    public ResponseEntity<String> createJob(@RequestBody JobRequestDTO dto){
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
    public ResponseEntity<String> draftJob(@RequestBody JobRequestDTO dto){
        return ResponseEntity.ok(jobService.draftJob(dto));
    }

//    For -> client
//    Get Draft Job
    @GetMapping("/draft-job/{draftId}")
    public ResponseEntity<JobRequestDTO> getDraft(@PathVariable String draftId){
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

//    For -> Client
//    Get all draft Job
    @GetMapping("/draft-jobs")
    public ResponseEntity<List<JobRequestDTO>> getAllDraftJob(){
        return ResponseEntity.of(Optional.ofNullable(jobService.getAllDraft()));
    }

//    For -> Client
//    Edit published job
    @PutMapping("/edit-job/{id}")
    public ResponseEntity<String> editJob(@PathVariable Long id, @RequestBody JobRequestDTO dto){
        return ResponseEntity.ok(jobService.editJob(dto,id));
    }

//    For -> Client
//    Edit published job
    @PutMapping("/update-draft-job")
    public ResponseEntity<String> updateJob(@RequestBody JobRequestDTO dto){
        return ResponseEntity.ok(jobService.updateDraftJob(dto));
    }
}