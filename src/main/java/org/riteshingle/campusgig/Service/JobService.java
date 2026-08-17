package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.riteshingle.campusgig.Enum.*;
import org.riteshingle.campusgig.Model.*;
import org.riteshingle.campusgig.Repository.JobApplicationRepository;
import org.riteshingle.campusgig.Repository.JobRepository;
import org.riteshingle.campusgig.Repository.UserSkillsRepository;
import org.riteshingle.campusgig.RequestDTO.JobRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.GigResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.JobApplicantResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.JobResponseDTO;
import org.riteshingle.campusgig.Specification.JobSpecification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {
    private final JobRepository jobRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final UserSkillsRepository userSkillsRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    //    For -> client
//    publish Job
    public String publishJob(JobRequestDTO dto) {
//        Get Current logged-in user profile
        UserEntity client = authService.getCurrentProfile();
//        Check client is verified or not
        if (!client.getIsVerified()) throw new RuntimeException("Client is not Verified");

//        Validate All
        validateDraft(dto);

//        Create Job Entity
        Job job = createJobEntity(dto);

//        Check Experience , Work mode and Job status is valid or not ?
        ExperienceLevel experienceLevel = ExperienceLevel.valueOf(dto.getExperienceLevel().trim().toUpperCase());
        JobStatus status = JobStatus.valueOf(dto.getJobStatus().trim().toUpperCase());
        WorkMode workMode = WorkMode.valueOf(dto.getWorkMode().trim().toUpperCase());
        JobCategory category = JobCategory.valueOf(dto.getJobCategory().trim().toUpperCase());

        job.setExperienceLevel(experienceLevel);
        job.setWorkMode(workMode);
        job.setJobStatus(status);
        job.setCategory(category);
        job.setUser(client);

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
        UserEntity client = authService.getCurrentProfile(); //Get Current logged-in profile

        if (!client.getIsVerified()) throw new RuntimeException("You are not varified ..");

        validateDraft(dto);  //Check Experience , Work mode and Job status is valid or not ?

        try {
            String draftId = UUID.randomUUID().toString();  //Create random draft ID
            String key = "job:draft:" + client.getId() + ":" + draftId;  //creating redis key using userId and draftId
            String json = objectMapper.writeValueAsString(dto);  //Convert CreateJobRequestDTO object in String JSON

            redisTemplate.opsForValue().set(key, json, Duration.ofDays(7));  //set key and object in redis with 7 days TTL
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

        if (!client.getIsVerified()) throw new RuntimeException("You are not varified ..");

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

        if (!client.getIsVerified()) throw new RuntimeException("You are not varified ..");

        String key = "job:draft:" + client.getId() + ":" + draftId;

//        Check is key deleted ?
        try {
            Boolean delete = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(delete)) {
                log.info("Draft job deleted..");
            } else {
                log.warn("Draft key not found (already expired or never existed): {}", key);
            }
        } catch (Exception e) {
            log.error("Failed to delete draft from Redis: {}", key, e);
        }
    }

    //    For -> client
//    Delete Job
//    Soft delete
    public String deleteJob(Long id) {
//        Get current logged-in user profile
        UserEntity client = authService.getCurrentProfile();

//        Check client is verified or not
        if (!client.getIsVerified()) throw new RuntimeException("Client is not Verified");

//        find job by job id
        Job job = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Job not found with : " + id));

//        check job client
        if (job.getUser().getId().equals(client.getId())) {
//            Change Job status to DELETED and save in DB
            job.setJobStatus(JobStatus.DELETED);
            jobRepository.save(job);
//            jobRepository.delete(job);
            return "Job deleted";
        } else throw new RuntimeException("You are not authorized to delete this job");
    }

    //    For -> Client
//    Get All Draft
    public List<JobRequestDTO> getAllDraft() {
//        Get Current logged-in user profile
        UserEntity client = authService.getCurrentProfile();
//        Check user is verified or not
        if (!client.getIsVerified()) throw new RuntimeException("You are not varified ..");

//        Creating key pattern for scan in redis
        String pattern = "job:draft:" + client.getId() + ":*";
//        Get All similar keys from redis
        Set<String> keys = scanKeys(pattern);

//        Empty list for store json
        List<JobRequestDTO> draftJobList = new ArrayList<>();
//        Travers on set
        for (String key : keys) {
            String json = Objects.requireNonNull(redisTemplate.opsForValue().get(key)).toString();
            if (json == null) {
                continue;
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
    public String editJob(JobRequestDTO dto, Long jobId) {
        UserEntity client = authService.getCurrentProfile();

//        Check client is verified or not
        if (!client.getIsVerified()) throw new RuntimeException("Client is not Verified");

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));

        // Ownership check
        if (!job.getUser().getId().equals(client.getId()))
            throw new RuntimeException("You are not authorized to edit this job");

//        Validate enums
        validateDraft(dto);

        if (dto.getTitle() != null) job.setTitle(dto.getTitle());

        if (dto.getDescription() != null) job.setDescription(dto.getDescription());

        if (dto.getDeadline() != null) job.setDeadline(dto.getDeadline());

        if (dto.getBudget() != null) job.setBudget(dto.getBudget());

        ExperienceLevel experienceLevel = ExperienceLevel.valueOf(dto.getExperienceLevel().trim().toUpperCase());
        WorkMode workMode = WorkMode.valueOf(dto.getWorkMode().trim().toUpperCase());
        JobStatus jobStatus = JobStatus.valueOf(dto.getJobStatus().trim().toUpperCase());
        JobCategory jobCategory = JobCategory.valueOf(dto.getJobCategory().trim().toUpperCase());

        if (dto.getWorkMode() != null) job.setWorkMode(workMode);
        if (dto.getJobStatus() != null) job.setJobStatus(jobStatus);
        if (dto.getJobCategory() != null) job.setCategory(jobCategory);
        if (dto.getExperienceLevel() != null) job.setExperienceLevel(experienceLevel);

        jobRepository.save(job);
        return "Job edited !";
    }

    //    For -> Client
//    Update Draft Job
    public String updateDraftJob(JobRequestDTO dto) {
        UserEntity client = authService.getCurrentProfile();

//        Check user is verified or not
        if (!client.getIsVerified()) throw new RuntimeException("You are not varified ..");

        String draftId = dto.getDraftId();
        String key = "job:draft:" + client.getId() + ":" + draftId;

//        check draft DTO is Valid or not
        validateDraft(dto);

        try {
//            Map object in String
            String json = objectMapper.writeValueAsString(dto);
//            Set in redis
            redisTemplate.opsForValue().set(key, json, Duration.ofDays(7));
            return "Draft updated";
        } catch (Exception e) {
            throw e;
        }
    }

    //    For -> Client
//    Get All Job Applicant
    public List<JobApplicantResponseDTO> getAllJobApplicant(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        List<JobApplication> jobApplicant = jobApplicationRepository.findByJob(job);
        return jobApplicant.stream().map(this::jobApplicantResponse).toList();
    }

//    For -> Client
//    Get all job posted by client
    public List<JobResponseDTO> getAllJobsPostByMe(String status) {
        UserEntity client = authService.getCurrentProfile();
        JobStatus jobStatus;

        try {
            jobStatus = JobStatus.valueOf(status.toUpperCase().trim());
        }catch (Exception e){
            throw e;
        }

        List<Job> jobs = jobRepository.findJobsByClientIdAndStatus(client.getId(),jobStatus);
        return jobs.stream().map(this::responseDTO).toList();
    }

//    For -> client
//    Accept Job Application
    public void acceptJobProposal(Long jobId, Long applicationId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found ..."));
        JobApplication jobApplication = jobApplicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("Job application not found .."));
        UserEntity currentProfile = authService.getCurrentProfile();

        if (!currentProfile.getId().equals(job.getUser().getId())) {
            throw new RuntimeException("Your not authorized to accept the job proposal ..");
        }

        if (!jobApplication.getJob().getId().equals(jobId)) {
            throw new RuntimeException("This application does not belong to this job..");
        }

        if(jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.ACCEPTED)){
            throw new RuntimeException("Application is already  accepted ..");
        }

        List<JobApplication> applicantsList = jobApplicationRepository.findByJob(job);
        for (JobApplication application : applicantsList) {
            if((application.getJobApplicationStatus().equals(JobApplicationStatus.APPLIED)
                    || application.getJobApplicationStatus().equals(JobApplicationStatus.SHORTLISTED))
                    && !jobApplication.getId().equals(application.getId()))
            {
                application.setJobApplicationStatus(JobApplicationStatus.REJECTED);
            }
        }

        jobApplicationRepository.saveAll(applicantsList);

        jobApplication.setJobApplicationStatus(JobApplicationStatus.ACCEPTED);
        job.setJobStatus(JobStatus.CLOSED);
        jobRepository.save(job);
        jobApplicationRepository.save(jobApplication);
    }

    public void rejectJobProposal(Long applicationId){
        UserEntity client = authService.getCurrentProfile();
        JobApplication jobApplication = jobApplicationRepository.findById(applicationId).orElseThrow(() -> new RuntimeException("Job application not found .."));

        Long clientId = jobApplication.getJob().getUser().getId();

        if(!client.getId().equals(clientId)){
            throw new RuntimeException("Your not authorized to reject the Job Application ..");
        }

        if(jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.REJECTED)
                || jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.ACCEPTED)
                || jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.WITHDRAWN)){
            throw new RuntimeException("Job Application is already "+jobApplication.getJobApplicationStatus()+"..");
        }

        jobApplication.setJobApplicationStatus(JobApplicationStatus.REJECTED);
        jobApplicationRepository.save(jobApplication);
    }

//    helper methods
//    Validate Draft Object and Publish Object
    public void validateDraft(JobRequestDTO dto) {
        List<String> errors = new ArrayList<>();

//        Check Experience level
        if (dto.getExperienceLevel() != null)
            if (isValidEnum(ExperienceLevel.class, dto.getExperienceLevel()))
                errors.add("Invalid experience level: " + dto.getExperienceLevel() +
                        ". Allowed: " + Arrays.toString(ExperienceLevel.values()));

//        Check Job Category
        if (dto.getJobCategory() != null)
            if (isValidEnum(JobCategory.class, dto.getJobCategory()))
                errors.add("Invalid experience level: " + dto.getJobCategory() +
                        ". Allowed: " + Arrays.toString(ExperienceLevel.values()));

//        Check work mode
        if (dto.getWorkMode() != null)
            if (isValidEnum(WorkMode.class, dto.getWorkMode()))
                errors.add("Invalid work mode: " + dto.getWorkMode() +
                        ". Allowed: " + Arrays.toString(WorkMode.values()));

//        Check Job Status
        if (dto.getJobStatus() != null)
            if (isValidEnum(JobStatus.class, dto.getJobStatus()))
                errors.add("Invalid job status: " + dto.getJobStatus() +
                        ". Allowed: " + Arrays.toString(JobStatus.values()));

//        Check title size
        if (dto.getTitle() != null && dto.getTitle().length() > 200)
            errors.add("Title too long, max 200 characters allowed");

//        Check description size
        if (dto.getDescription() != null && dto.getDescription().length() > 5000)
            errors.add("Description too long, max 5000 characters allowed");

        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("; ", errors));
    }

    //    Job response DTO
    private JobResponseDTO responseDTO(Job job) {
        return JobResponseDTO.builder()
                .budget(job.getBudget())
                .publishAt(job.getPublishAt())
                .title(job.getTitle())
                .description(job.getDescription())
                .workMode(job.getWorkMode().name())
                .category(job.getCategory().name())
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
        if (value == null || value.isBlank()) return true;
        try {
            Enum.valueOf(enumClass, value.trim().toUpperCase());
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    @Transactional(readOnly = true)
    private JobApplicantResponseDTO jobApplicantResponse(JobApplication jobApplication) {
        GIG gig = jobApplication.getGig();
        UserEntity user = gig.getUser();

        GigResponseDTO gigResponseDTO = GigResponseDTO.builder()
                .gigFirstName(user.getFirstName())
                .gigLastName(user.getLastName())
                .gigEmail(user.getEmail())
                .gigPhoneNumber(user.getPhoneNumber())
                .title(gig.getTitle())
                .description(gig.getDescription())
                .jobCategory(gig.getJobCategory())
                .availabilityStatus(gig.getAvailabilityStatus())
                .build();

        List<String> skills = userSkillsRepository.findSkillByGigId(gig.getId());
        gigResponseDTO.setGigSkills(skills);

        return JobApplicantResponseDTO.builder()
                .jobApplicationStatus(jobApplication.getJobApplicationStatus().name())
                .bidAmount(jobApplication.getBidAmount())
                .coverLetter(jobApplication.getCoverLetter())
                .deliveryDate(jobApplication.getDeliveryDate())
                .applyAt(jobApplication.getCreateAt())
                .gigResponseDTO(gigResponseDTO)
                .build();
    }
}