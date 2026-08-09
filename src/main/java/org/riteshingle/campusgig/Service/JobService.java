package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.riteshingle.campusgig.Enum.ExperienceLevel;
import org.riteshingle.campusgig.Enum.JobStatus;
import org.riteshingle.campusgig.Enum.WorkMode;
import org.riteshingle.campusgig.Model.Job;
import org.riteshingle.campusgig.Model.JobCategory;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.Repository.JobCategoryRepository;
import org.riteshingle.campusgig.Repository.JobRepository;
import org.riteshingle.campusgig.RequestDTO.CreateJobRequestDTO;
import org.riteshingle.campusgig.RequestDTO.SaveDraftRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.JobResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {
    private final JobCategoryRepository jobCategoryRepository;
    private final JobRepository jobRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

//    For -> client
//    publish Job
    public String publishJob(CreateJobRequestDTO dto) {
//        Get Current logged-in user profile
        UserEntity client = authService.getCurrentProfile();
//        find Category is valid or not
        JobCategory jobCategory = jobCategoryRepository.findByCategory(dto.getCategory()).orElseThrow(() -> new RuntimeException("Category not found"));

//        Check Experience , Work mode and Job status is valid or not ?
        ExperienceLevel experienceLevel = Enum.valueOf(ExperienceLevel.class, dto.getExperienceLevel().trim().toUpperCase());
        WorkMode workMode = Enum.valueOf(WorkMode.class, dto.getWorkMode().trim().toUpperCase());
        JobStatus status = JobStatus.valueOf(dto.getJobStatus().toUpperCase().trim());

//        Create Job Entity
        Job job = createJobEntity(dto);
        job.setUser(client);
        job.setCategory(jobCategory);
        job.setExperienceLevel(experienceLevel);
        job.setWorkMode(workMode);
        job.setJobStatus(status);

//        save in DB
        jobRepository.save(job);

//        Check is draft ID is null or not ?
        if (dto.getDraftId() != null) {
//            delete key from redis/Draft
            String key = "job:draft:" + client.getId() + ":" + dto.getDraftId();
            redisTemplate.delete(key);
        }

        return "Job Published!";
    }

//    For -> GIG
//    Get All Jobs
    public List<JobResponseDTO> getJobs(Pageable pageable) {
//        Fetch all jobs
        List<Job> jobs = jobRepository.findAll(pageable).getContent();
//        return in Job response DTO list
        return jobs.stream().map(this::responseDTO).toList();
    }

//    For -> GIG
//    Get a job by ID
    public JobResponseDTO getJob(Long id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Job not found with : " + id));
        return responseDTO(job);
    }

//    For -> client
//    Add job in Draft
    public String draftJob(CreateJobRequestDTO dto) {
//        Get Current logged-in profile
        UserEntity client = authService.getCurrentProfile();
//        Check Job category is valid or not
        JobCategory jobCategory;
        if (dto.getCategory() != null) {
            jobCategory = jobCategoryRepository.findByCategory(dto.getCategory()).orElseThrow(() -> new RuntimeException("Job Category not found.."));
        }

//        Check Experience , Work mode and Job status is valid or not ?
        ExperienceLevel experienceLevel = ExperienceLevel.valueOf(dto.getExperienceLevel().trim().toUpperCase());
        WorkMode workMode = WorkMode.valueOf(dto.getWorkMode().trim().toUpperCase());
        JobStatus status = JobStatus.valueOf(dto.getJobStatus().toUpperCase().trim());

//        ExperienceLevel experienceLevel;
//        WorkMode workMode;
//        JobStatus status;
//
//        try {
//            if (dto.getExperienceLevel() != null)
//                experienceLevel = ExperienceLevel.valueOf(dto.getExperienceLevel().trim().toUpperCase());
//
//            if (dto.getWorkMode() != null)
//              workMode = WorkMode.valueOf(dto.getWorkMode().trim().toUpperCase());
//
//            if (dto.getJobStatus() != null)
//                status = JobStatus.valueOf(dto.getJobStatus().toUpperCase().trim());
//        } catch (Exception e) {
//            throw new RuntimeException("work mode or experience level and job status not found...");
//        }

        try {
//            Create random draft ID
            String draftId = UUID.randomUUID().toString();
//            creating redis key using userId and draftId
            String key = "job:draft:" + client.getId() + ":" + draftId;
//            Convert CreateJobRequestDTO object in String JSON
            String json = objectMapper.writeValueAsString(dto);
//            set key and object in redis with 7 days TTL
            redisTemplate.opsForValue().set(key, json, Duration.ofDays(7));
            return "Job saved in draft ,\nDraftId : " + draftId;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    For -> client
//    Get Draft
    public CreateJobRequestDTO getDraft(String draftId) {
//        get current logged-in user profile
        UserEntity client = authService.getCurrentProfile();
//        create key for redis
        String key = "job:draft:" + client.getId() + ":" + draftId;
//        getting value not null
        String json = Objects.requireNonNull(redisTemplate.opsForValue().get(key)).toString();

//        Check
        if (json == null) {
            return null;
        }

        try {
//            return value in JSON
            return objectMapper.readValue(json, CreateJobRequestDTO.class);
        } catch (Exception e) {
            throw e;
        }
    }

//    For -> client
//    Remove Draft
    public void removeDraft(String draftId) {
//        Get current logged-in user profile
        UserEntity client = authService.getCurrentProfile();
        String key = "job:draft:" + client.getId() + ":" + draftId;

//        Check is key deleted ?
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.error("Draft job deleted..");
            } else {
                log.warn("Draft key not found (already expired or never existed): {}", key);
            }
        } catch (Exception e) {
            log.error("Failed to delete draft from Redis: {}", key, e); // best-effort, swallow kiya
        }
    }

//    For -> client
//    Delete Job
//    Soft delete
    public String deleteJob(Long id) {
//        Get current logged-in user profile
        UserEntity client = authService.getCurrentProfile();
//        find job by job id
        Job job = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Job not found with : " + id));

//        check job client
        if (job.getUser().getId().equals(client.getId())) {
//            Change Job status to DELETED and save in DB
            job.setJobStatus(JobStatus.DELETED);
            jobRepository.save(job);
//            jobRepository.delete(job);
            return "Job deleted";
        } else {
            throw new RuntimeException("You are not authorized to delete this job");
        }
    }

    //    helper methods

//    Job response DTO
    private JobResponseDTO responseDTO(Job job) {
        return JobResponseDTO.builder()
                .budget(job.getBudget())
                .publishAt(job.getPublishAt())
                .title(job.getTitle())
                .description(job.getDescription())
                .workMode(job.getWorkMode().name())
                .category(job.getCategory().getCategory())
                .deadline(job.getDeadline())
                .experience(job.getExperienceLevel().name())
                .jobStatus(job.getJobStatus().name())
                .build();
    }

//    Job Entity
    private Job createJobEntity(CreateJobRequestDTO dto) {
        return Job.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .budget(dto.getBudget())
                .deadline(dto.getDeadline())
                .build();
    }
}