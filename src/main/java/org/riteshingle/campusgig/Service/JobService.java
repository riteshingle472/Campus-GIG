package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.riteshingle.campusgig.Enum.*;
import org.riteshingle.campusgig.Exception.*;
import org.riteshingle.campusgig.Model.*;
import org.riteshingle.campusgig.Repository.*;
import org.riteshingle.campusgig.RequestDTO.JobRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.GigResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.SkillResponseDTO;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {
    private final JobRepository jobRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final ContractService contractService;
    private final UserSkillsRepository userSkillsRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ContractRepository contractRepository;
    private final ConversationRepository conversationRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationService notificationService;


//    For -> client
//    publish Job
    public void publishJob(JobRequestDTO dto) {
//        Get Current logged-in user profile
        UserEntity client = authService.getCurrentProfile();
//        Check client is verified or not
        if (!client.getIsVerified()) throw new ResourceNotFoundException("Client is not Verified");

//        Validate All
        validateDraft(dto);

//        Create Job Entity
        Job job = createJobEntity(dto);

//        Validate DeadLine
        if (dto.getDeadline() == null) throw new BadRequestException("Deadline is required");
        if (dto.getDeadline().isBefore(LocalDate.now())) throw new BadRequestException("Deadline cannot be in the past");

        job.setClient(client);
//        job.setJobStatus(JobStatus.OPEN);

//        Check Experience , Work mode and Job status is valid or not ?
       try{
           ExperienceLevel experienceLevel = ExperienceLevel.valueOf(dto.getExperienceLevel().trim().toUpperCase());
           job.setExperienceLevel(experienceLevel);
       }catch (Exception e){
            throw new InvalidStatusException("Invalid Experience level : "+dto.getJobCategory());
       }

        try{
            JobStatus status = JobStatus.valueOf(dto.getJobStatus().trim().toUpperCase());

            if(status.equals(JobStatus.DRAFT) || status.equals(JobStatus.CLOSED))
                throw new InvalidStatusException("Invalid Job Status : "+status.name());

            job.setJobStatus(status);
        }catch (Exception e) {
            throw new InvalidStatusException("Invalid Job Status: " + dto.getJobStatus());
        }

        try{
            JobCategory category = JobCategory.valueOf(dto.getJobCategory().trim().toUpperCase());
            job.setCategory(category);
        }catch (Exception e){
            throw new InvalidStatusException("Invalid Job Category : "+dto.getExperienceLevel());
        }

//        save in DB
        jobRepository.save(job);

//        Check draft ID is null or not ?
        if (dto.getDraftId() != null) {
//            delete key from redis/Draft
            String key = "job:draft:" + client.getId() + ":" + dto.getDraftId();
            redisTemplate.delete(key);
        }
    }

//    For -> GIG
//    Get All Jobs
    public List<JobResponseDTO> getJobs(Pageable pageable, BigDecimal min,BigDecimal max) {
        Specification<Job> specification = JobSpecification.hasStatus(JobStatus.OPEN);

        if (max != null) specification = specification.and(JobSpecification.maxBudget(max));
        if (min != null) specification = specification.and(JobSpecification.minBudget(min));

//        Fetch all jobs
        List<Job> jobs = jobRepository.findAll(specification,pageable).getContent();
        return jobs.stream().map(this::responseDTO).toList();
    }

//    For -> GIG
//    Get a job by ID
    public JobResponseDTO getJob(Long id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found with : " + id));
        return responseDTO(job);
    }

//    For -> client
//    Add job in Draft
    public void draftJob(JobRequestDTO dto) {
//        Get Current logged-in profile
        UserEntity client = authService.getCurrentProfile();

//        Check Profile is verified or not
        if (!client.getIsVerified()) throw new ForbiddenException("You are not varified ..");

        validateDraft(dto);  //Check Experience , Work mode and Job status is valid or not ?

        try {
            String draftId = UUID.randomUUID().toString();  //Create random draft ID
            String key = "job:draft:" + client.getId() + ":" + draftId;  //creating redis key using userId and draftId
            dto.setDraftId(draftId);
            String json = objectMapper.writeValueAsString(dto);  //Convert CreateJobRequestDTO object in String JSON
            redisTemplate.opsForValue().set(key, json, Duration.ofDays(7));  //set key and object in redis with 7 days TTL
        } catch (Exception e) {
            throw new DraftSaveException("Failed to serialize draft");
        }
    }

//    For -> client
//    Get Draft
    public JobRequestDTO getDraft(String draftId) {
//        get current logged-in user profile
        UserEntity client = authService.getCurrentProfile();

//        Check profile is verified or not
        if (!client.getIsVerified()) throw new ForbiddenException("You are not varified ..");

//        create key for redis
        String key = "job:draft:" + client.getId() + ":" + draftId;

//        get value not null
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) throw new ResourceNotFoundException("Draft not found for key: " + key);

//        Convert value in String
        String json = value.toString();

        try {
//            return value in JSON
            return objectMapper.readValue   (json, JobRequestDTO.class);
        } catch (Exception e) {
            throw new DraftException("Failed to parse json..");
        }
    }

//    For -> client
//    Remove Draft
    public void removeDraft(String draftId) {
//        Get current logged-in user profile
        UserEntity client = authService.getCurrentProfile();

        if (!client.getIsVerified()) throw new ForbiddenException("You are not varified ..");

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
            throw new DraftException("Failed to delete draft from Redis:");
        }
    }

//    For -> client
//    Delete Job
//    Soft delete
    public void deleteJob(Long id) {
//        Get current logged-in user profile
        UserEntity client = authService.getCurrentProfile();

//        Check client is verified or not
        if (!client.getIsVerified()) throw new ForbiddenException("Client is not Verified");

//        find job by job id
        Job job = jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found with : " + id));

//        check job client
        if (job.getClient().getId().equals(client.getId())) {
//            Change Job status to DELETED and save in DB
            job.setJobStatus(JobStatus.DELETED);
            jobRepository.save(job);
//            jobRepository.delete(job);
        } else throw new ForbiddenException("You are not authorized to delete this job");
    }

//    For -> Client
//    Get All Draft
    public List<JobRequestDTO> getAllDraft() {
//        Get Current logged-in user profile
        UserEntity client = authService.getCurrentProfile();
//        Check user is verified or not
        if (!client.getIsVerified()) throw new ForbiddenException("You are not varified ..");

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
                throw new DraftException("Failed to parse draft for key:");
            }
        }
        return draftJobList;
    }

//    For -> Client
//    Edit Job
    public void editJob(JobRequestDTO dto, Long jobId) {
        UserEntity client = authService.getCurrentProfile();

//        Check client is verified or not
        if (!client.getIsVerified()) throw new ForbiddenException("Client is not Verified");

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        validateDraft(dto);

        if(job.getJobStatus().equals(JobStatus.DELETED) || job.getJobStatus().equals(JobStatus.DRAFT))
            throw new BadRequestException("Invalid Job status selection. Job is : "+job.getJobStatus());

        // Ownership check
        if (!job.getClient().getId().equals(client.getId())) throw new ForbiddenException("You are not authorized to edit this job");

        if (dto.getTitle() != null) job.setTitle(dto.getTitle());
        if (dto.getDescription() != null) job.setDescription(dto.getDescription());
        if (dto.getDeadline() != null) job.setDeadline(dto.getDeadline());
        if (dto.getBudget() != null) job.setBudget(dto.getBudget());


        ExperienceLevel experienceLevel;
        if(dto.getExperienceLevel() != null){
            try{
                experienceLevel = ExperienceLevel.valueOf(dto.getExperienceLevel().trim().toUpperCase());
                job.setExperienceLevel(experienceLevel);
            }catch (Exception e){
                throw new InvalidStatusException("Invalid Experience Level : "+dto.getExperienceLevel());
            }
        }

        JobStatus jobStatus;
        if(dto.getJobStatus() != null){
            try{
                jobStatus = JobStatus.valueOf(dto.getJobStatus().trim().toUpperCase());
                job.setJobStatus(jobStatus);
            }catch (Exception e){
                throw new InvalidStatusException("Invalid Job status : "+dto.getJobStatus());
            }
        }

        JobCategory jobCategory;
        if(dto.getJobCategory() != null){
            try{
                jobCategory = JobCategory.valueOf(dto.getJobCategory().trim().toUpperCase());
                job.setCategory(jobCategory);
            }catch (Exception e){
                throw new InvalidStatusException("Invalid Job Category : "+dto.getJobCategory());
            }
        }

        jobRepository.save(job);
    }

//    For -> Client
//    Update Draft Job
    public void updateDraftJob(JobRequestDTO dto) {
        UserEntity client = authService.getCurrentProfile();

//        Check user is verified or not
        if (!client.getIsVerified()) throw new ForbiddenException("You are not varified ..");

        String draftId = dto.getDraftId();
        String key = "job:draft:" + client.getId() + ":" + draftId;

//        check draft DTO is Valid or not
        validateDraft(dto);

        try {
//            Map object in String
            String json = objectMapper.writeValueAsString(dto);
//            Set in redis
            redisTemplate.opsForValue().set(key, json, Duration.ofDays(7));
        } catch (Exception e) {
            throw new DraftException("Failed to update Draft..");
        }
    }

//    For -> Client
//    Get All Job Applicant
    public List<JobApplicantResponseDTO> getAllJobApplicants(Long jobId,Pageable pageable,String keyword) {
//        Get Current logged-in profile
        UserEntity currentProfile = authService.getCurrentProfile();
//        Get Job by ID
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

//        Check Profile is verified or not
        if(!currentProfile.getIsVerified())
           throw new ForbiddenException("You are not authorized to view applicants — please verify your account.");

//        Check Client Authority
        if(!currentProfile.getId().equals(job.getClient().getId()))
            throw new UnauthorizedException("You are not authorized to modify this job");

//        Validate Job Application Status
        JobApplicationStatus status;
        try {
            status = JobApplicationStatus.valueOf(keyword.trim().toUpperCase());
        }catch (Exception e){
            throw new InvalidStatusException("Invalid Job Application Status : "+keyword);
        }

//        Fetch All job applicants by Job and Job Application Status
        List<JobApplication> jobApplicant = jobApplicationRepository.findJobApplicants(job,pageable,status).getContent();
        return jobApplicant.stream().map(this::jobApplicantResponse).toList();
    }

//    For -> Client
//    Get all job posted by client
    public List<JobResponseDTO> getAllJobsPostByMe(String status, Pageable pageable) {
//        Get Current logged-in profile
        UserEntity client = authService.getCurrentProfile();

//        Validate Job Status
        JobStatus jobStatus;
        try {jobStatus = JobStatus.valueOf(status.toUpperCase().trim());}
        catch (Exception e){throw new InvalidStatusException("Invalid Job Status : "+status);}

//        Fetch All Jobs
        List<Job> jobs = jobRepository.findJobsByClientIdAndStatus(client.getId(), jobStatus, pageable).getContent();
        return jobs.stream().map(this::responseDTO).toList();
    }

//    For -> client
//    Accept Job Application
    public void acceptJobProposal(Long jobId, Long applicationId) {
//        Get Job by ID
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found by ID : "+jobId));
//        Get Job Application by ID
        JobApplication jobApplication = jobApplicationRepository.findById(applicationId).orElseThrow(() -> new ResourceNotFoundException("Job application not found by ID : "+applicationId));
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

//        Check Client Authority
        if (!currentProfile.getId().equals(job.getClient().getId()))
            throw new ForbiddenException("Your not authorized to accept the job proposal ..");

//        Check JobApplication belong to Job
        if (!jobApplication.getJob().getId().equals(jobId))
            throw new ForbiddenException("This application does not belong to this job..");

        if(jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.ACCEPTED) ||
        jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.REJECTED) ||
        jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.WITHDRAWN)){
            throw new InvalidStatusException("Application is already "+jobApplication.getJobApplicationStatus()+"...");
        }

//        Reject All Proposal except one
        List<JobApplication> applicantsList = jobApplicationRepository.findByJob(job);
        for (JobApplication application : applicantsList) {
            if((application.getJobApplicationStatus().equals(JobApplicationStatus.APPLIED)
                    || application.getJobApplicationStatus().equals(JobApplicationStatus.SHORTLISTED))
                    && !jobApplication.getId().equals(application.getId()))
            {
                application.setJobApplicationStatus(JobApplicationStatus.REJECTED);
            }
        }

//        Save all rejected Proposals in DB
        jobApplicationRepository.saveAll(applicantsList);

//        Set Job Closed and save it in DB
        job.setJobStatus(JobStatus.CLOSED);
        jobRepository.save(job);

//        Set Job Application Accepted and save in DB
        jobApplication.setJobApplicationStatus(JobApplicationStatus.ACCEPTED);
        jobApplicationRepository.save(jobApplication);

//        Creating contract and conversation or chat
        Contract contract = contractService.createContract(job,jobApplication);
        contractRepository.save(contract);

        Conversation conversation = Conversation.builder().contract(contract).build();
        conversationRepository.save(conversation);
        notificationService.notify(
                contract.getGig().getUser(),
                NotificationType.CONTRACT_CREATED,
                "New Contract Created",
                "A new contract has been created for \"" + job.getTitle() + "\"",
                contract.getId()
        );
    }

//    Reject Proposals
    public void rejectJobProposal(Long applicationId){
//        Get Current Logged-in Profile
        UserEntity client = authService.getCurrentProfile();
//        Get Job Application by ID
        JobApplication jobApplication = jobApplicationRepository.findById(applicationId).orElseThrow(() -> new ResourceNotFoundException("Job application not found .."));

//        Get Client ID
        Long clientId = jobApplication.getJob().getClient().getId();

//        Verify Client
        if(!client.getId().equals(clientId)){
            throw new ForbiddenException("Your not authorized to reject the Job Application ..");
        }

        if(jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.REJECTED)
                || jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.ACCEPTED)
                || jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.WITHDRAWN)){
            throw new InvalidStatusException("Job Application is already "+jobApplication.getJobApplicationStatus()+"..");
        }

//        Reject Proposal
        jobApplication.setJobApplicationStatus(JobApplicationStatus.REJECTED);
        jobApplicationRepository.save(jobApplication);
    }

//    For -> client
//    Shortlist Job Application
    public void shortlistJobProposal(Long applicationId) {
//        Get Current Logged-in Profile
        UserEntity client = authService.getCurrentProfile();
//        Get Job Application by ID
        JobApplication jobApplication = jobApplicationRepository.findById(applicationId).orElseThrow(() -> new ResourceNotFoundException("Job application not found .."));
//        Get Client ID
        Long clientId = jobApplication.getJob().getClient().getId();

//        Verify Client
        if (!client.getId().equals(clientId))
            throw new ForbiddenException("You're not authorized to shortlist this proposal ..");

        if (jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.REJECTED)
                || jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.ACCEPTED)
                || jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.WITHDRAWN)
                || jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.SHORTLISTED)) {
            throw new InvalidStatusException(
                    "Job Proposal is already " + jobApplication.getJobApplicationStatus() + "..");
        }

        jobApplication.setJobApplicationStatus(JobApplicationStatus.SHORTLISTED);
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
                errors.add("Invalid job category: " + dto.getJobCategory() +
                        ". Allowed: " + Arrays.toString(JobCategory.values()));

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
                .id(job.getId())
                .budget(job.getBudget())
                .publishAt(job.getPublishAt())
                .title(job.getTitle())
                .description(job.getDescription())
                .category(job.getCategory().name())
                .deadline(job.getDeadline())
                .experience(job.getExperienceLevel().name())
                .jobStatus(job.getJobStatus().name())
                .clientFirstName(job.getClient().getFirstName())   // NEW
                .clientLastName(job.getClient().getLastName())     // NEW
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
                .id(gig.getId())                          // NEW
                .gigFirstName(user.getFirstName())
                .gigLastName(user.getLastName())
                .gigEmail(user.getEmail())
                .gigPhoneNumber(user.getPhoneNumber())
                .description(gig.getDescription())
                .title(gig.getTitle())
                .college(gig.getCollege())
                .department(gig.getDepartment())
                .semester(gig.getSemester())
                .availabilityStatus(gig.getAvailabilityStatus())
                .build();

        List<String> skillNames = userSkillsRepository.findSkillByGigId(gig.getId());

        List<SkillResponseDTO> skills = skillNames.stream()
                .map(skillName -> SkillResponseDTO.builder()
                        .skill(skillName)
                        .build())
                .toList();

        gigResponseDTO.setGigSkills(skills);

        return JobApplicantResponseDTO.builder()
                .id(jobApplication.getId())
                .jobApplicationStatus(jobApplication.getJobApplicationStatus().name())
                .bidAmount(jobApplication.getBidAmount())
                .coverLetter(jobApplication.getCoverLetter())
                .deliveryDate(jobApplication.getDeliveryDate())
                .applyAt(jobApplication.getCreatedAt())
                .gigResponseDTO(gigResponseDTO)
                .build();

    }
}