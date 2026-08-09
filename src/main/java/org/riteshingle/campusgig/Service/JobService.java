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
import org.riteshingle.campusgig.RequestDTO.JobRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.JobResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.*;

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
    public String publishJob(JobRequestDTO dto) {
//        Get Current logged-in user profile
        UserEntity client = authService.getCurrentProfile();
//        Check client is verified or not
        if(!client.getIsVerified()) throw new RuntimeException("Client is not Verified");

//        find Category is valid or not
        JobCategory jobCategory = jobCategoryRepository.findByCategory(dto.getCategory()).orElseThrow(() -> new RuntimeException("Category not found"));

//        Validate All
        validateDraft(dto);

//        Create Job Entity
        Job job = createJobEntity(dto);
        job.setUser(client);
        job.setCategory(jobCategory);

//        Check Experience , Work mode and Job status is valid or not ?
        if (dto.getExperienceLevel() != null) {
            try {
                ExperienceLevel experienceLevel = ExperienceLevel.valueOf(
                        dto.getExperienceLevel().trim().toUpperCase());
                job.setExperienceLevel(experienceLevel);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid experience level: " + dto.getExperienceLevel() +
                                ". Allowed values: " + Arrays.toString(ExperienceLevel.values()));
            }
        }

        if (dto.getWorkMode() != null) {
            try {
                WorkMode workMode = WorkMode.valueOf(dto.getWorkMode().trim().toUpperCase());
                job.setWorkMode(workMode);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid work mode: " + dto.getWorkMode() +
                                ". Allowed values: " + Arrays.toString(WorkMode.values()));
            }
        }

        if (dto.getJobStatus() != null) {
            try {
                JobStatus status = JobStatus.valueOf(dto.getJobStatus().trim().toUpperCase());
                job.setJobStatus(status);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid job status: " + dto.getJobStatus() +
                                ". Allowed values: " + Arrays.toString(JobStatus.values()));
            }
        }
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
    public String draftJob(JobRequestDTO dto) {
//        Get Current logged-in profile
        UserEntity client = authService.getCurrentProfile();
//        Check Job category is valid or not
        JobCategory jobCategory;
        if (dto.getCategory() != null) {
            jobCategory = jobCategoryRepository.findByCategory(dto.getCategory()).orElseThrow(() -> new RuntimeException("Job Category not found.."));
        }

//        Check Experience , Work mode and Job status is valid or not ?
        validateDraft(dto);


//        ExperienceLevel experienceLevel = ExperienceLevel.valueOf(dto.getExperienceLevel().trim().toUpperCase());
//        WorkMode workMode = WorkMode.valueOf(dto.getWorkMode().trim().toUpperCase());
//        JobStatus status = JobStatus.valueOf(dto.getJobStatus().toUpperCase().trim());

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
            throw new RuntimeException("Failed to serialize draft", e);
        }
    }

//    For -> client
//    Get Draft
    public JobRequestDTO getDraft(String draftId) {
//        get current logged-in user profile
        UserEntity client = authService.getCurrentProfile();
//        create key for redis
        String key = "job:draft:" + client.getId() + ":" + draftId;
//        get value not null
        String json = Objects.requireNonNull(redisTemplate.opsForValue().get(key)).toString();

//        Check
        if (json == null) {
            return null;
        }

        try {
//            return value in JSON
            return objectMapper.readValue(json, JobRequestDTO.class);
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

//        Check client is verified or not
        if(!client.getIsVerified()) throw new RuntimeException("Client is not Verified");

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

//    For -> Client
//    Get All Draft
    public List<JobRequestDTO> getAllDraft(){
//        Get Current logged-in user profile
        UserEntity client = authService.getCurrentProfile();
//        Creating key pattern for scan in redis
        String pattern = "job:draft:"+client.getId()+":*";
//        Get All similar keys from redis
        Set<String> keys = scanKeys(pattern);

//        Empty list for store json
        List<JobRequestDTO> draftJobList = new ArrayList<>();
//        Travers on set
        for(String key : keys){
            String json = redisTemplate.opsForValue().get(key).toString();
            if(json == null){
                return null;
            }
            try {
//                Convert Object in String json and add in list
                draftJobList.add(objectMapper.readValue(json, JobRequestDTO.class));
            } catch (Exception e) {
                log.error("Failed to parse draft for key: {}", key, e);
            }
        }
        return draftJobList;
    }

//    For -> Client
//    Edit Job
    public String editJob(JobRequestDTO dto,Long jobId){
        UserEntity client = authService.getCurrentProfile();

//        Check client is verified or not
        if(!client.getIsVerified()) throw new RuntimeException("Client is not Verified");

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));

        // Ownership check
        if (!job.getUser().getId().equals(client.getId())) {
            throw new RuntimeException("You are not authorized to edit this job");
        }

        validateDraft(dto);

        if (dto.getTitle() != null) {
            job.setTitle(dto.getTitle());
        }

        if (dto.getDescription() != null) {
            job.setDescription(dto.getDescription());
        }

        if (dto.getDeadline() != null) {
            job.setDeadline(dto.getDeadline());
        }

        if (dto.getBudget() != null) {
            job.setBudget(dto.getBudget());
        }


        ExperienceLevel experienceLevel = ExperienceLevel.valueOf(dto.getExperienceLevel().trim().toUpperCase());
        WorkMode workMode = WorkMode.valueOf(dto.getWorkMode().trim().toUpperCase());
        JobStatus jobStatus = JobStatus.valueOf(dto.getJobStatus().trim().toUpperCase());

        if (dto.getCategory() != null) {
            JobCategory jobCategory = jobCategoryRepository.findByCategory(dto.getCategory())
                    .orElseThrow(() -> new RuntimeException("Job Category not found: " + dto.getCategory()));
            job.setCategory(jobCategory);
        }

        if (dto.getJobStatus() != null) {
            job.setJobStatus(jobStatus);
        }

        if (dto.getExperienceLevel() != null) {
            job.setExperienceLevel(experienceLevel);
        }

        if (dto.getWorkMode() != null) {
            job.setWorkMode(workMode);
        }

        jobRepository.save(job);
        return "Job edited !";
    }

//    For -> Client
//    Update Draft Job
    public String updateDraftJob(JobRequestDTO dto){
        UserEntity client = authService.getCurrentProfile();
        String draftId = dto.getDraftId();
        String key = "job:draft:"+client.getId()+":"+draftId;

//        check draft DTO is Valid or not
        validateDraft(dto);

        try{
//            Map object in String
            String json = objectMapper.writeValueAsString(dto);
//            Set in redis
            redisTemplate.opsForValue().set(key,json,Duration.ofDays(7));
            return "Draft updated";
        }catch (Exception e){
            throw e;
        }
    }

//    Validate Draft Object and Publish Object
    public void validateDraft(JobRequestDTO dto) {
        List<String> errors = new ArrayList<>();

//        Check Experience levele
        if (dto.getExperienceLevel() != null) {
            if (!isValidEnum(ExperienceLevel.class, dto.getExperienceLevel())) {
                errors.add("Invalid experience level: " + dto.getExperienceLevel() +
                        ". Allowed: " + Arrays.toString(ExperienceLevel.values()));
            }
        }

//        Check work mode
        if (dto.getWorkMode() != null) {
            if (!isValidEnum(WorkMode.class, dto.getWorkMode())) {
                errors.add("Invalid work mode: " + dto.getWorkMode() +
                        ". Allowed: " + Arrays.toString(WorkMode.values()));
            }
        }

        if(dto.getCategory() != null){
            Boolean exists = jobCategoryRepository.existsByCategory(dto.getCategory());
            if(!exists){
                errors.add("Invalid Job Category: "+dto.getCategory()+". Allowed: ");
            }
        }

//        Check Job Status
        if (dto.getJobStatus() != null) {
            if (!isValidEnum(JobStatus.class, dto.getJobStatus())) {
                errors.add("Invalid job status: " + dto.getJobStatus() +
                        ". Allowed: " + Arrays.toString(JobStatus.values()));
            }
        }

//        Check title size
        if (dto.getTitle() != null && dto.getTitle().length() > 200) {
            errors.add("Title too long, max 200 characters allowed");
        }

//        Check description size
        if (dto.getDescription() != null && dto.getDescription().length() > 5000) {
            errors.add("Description too long, max 5000 characters allowed");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
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
    private Job createJobEntity(JobRequestDTO dto) {
        return Job.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .budget(dto.getBudget())
                .deadline(dto.getDeadline())
                .build();
    }

//    Redis scan method
    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

        assert redisTemplate.getConnectionFactory() != null;
        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection().scan(options)) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
        }
        return keys;
    }

    // Helper — enum valid
        private <T extends Enum<T>> boolean isValidEnum(Class<T> enumClass, String value) {
        if (value == null || value.isBlank()) return false;
        try {
            Enum.valueOf(enumClass, value.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}