package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.*;
import org.riteshingle.campusgig.Exception.BadRequestException;
import org.riteshingle.campusgig.Exception.ForbiddenException;
import org.riteshingle.campusgig.Exception.InvalidStatusException;
import org.riteshingle.campusgig.Exception.ResourceNotFoundException;
import org.riteshingle.campusgig.Model.*;
import org.riteshingle.campusgig.Repository.*;
import org.riteshingle.campusgig.RequestDTO.BecomeGigRequestDTO;
import org.riteshingle.campusgig.RequestDTO.JobApplicationFilterAndSortingRequestDTO;
import org.riteshingle.campusgig.RequestDTO.JobApplicationRequestDTO;
import org.riteshingle.campusgig.RequestDTO.UpdateJobApplicationRequestDTO;
import org.riteshingle.campusgig.ResponseDTO.GigResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.JobApplicationSortingAndFilteringResponseDTO;
import org.riteshingle.campusgig.ResponseDTO.SkillResponseDTO;
import org.riteshingle.campusgig.Specification.GigSpecification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class GigService {
    private final JobApplicationRepository jobApplicationRepository;
    private final AuthService authService;
    private final UserSkillsRepository userSkillsRepository;
    private final UserEntityRepository userEntityRepository;
    private final SkillsRepository skillsRepository;
    private final GigRepository gigRepository;
    private final NotificationService notificationService;
    private final JobRepository jobRepository;

//    Add User Skills
    @Transactional
    public void addSkills(List<Long> skillIds) {
//        Get current profile
        UserEntity currentProfile = authService.getCurrentProfile();

//        Check user is verified or not
        if(!currentProfile.getIsVerified())
            throw new ForbiddenException("User is not verified ..");

//        Check gig
        if(!currentProfile.getRoles().contains(Roles.GIG)) throw new ForbiddenException("Only gig can add skills ..");

        GIG gig = currentProfile.getGig();

//        Get GIG existing skills
        List<Long> userExistingSkills = userSkillsRepository.findSkillIdsByGigId(gig.getId());
//        Filter skills from existing skills
        List<Long> newSkills = skillIds.stream().distinct().filter(id -> !userExistingSkills.contains(id)).toList();

//        If List is empty then do nothing
        if (newSkills.isEmpty()) return;

//        Find Skills by ID in List
        List<Skills> skills = skillsRepository.findAllById(newSkills);

//        Skill list size and Distinct skill list size if both are different then throw Exception Invalid skill selection
        if (skills.size() != newSkills.size())
            throw new BadRequestException("Invalid skill selected");

//        Setting skills in current logged-in user profile
        List<UserSkills> userSkills = skills.stream().map(skill -> new UserSkills(gig, skill)).toList();
        gig.getUserSkills().addAll(userSkills);

//        save gig in DB
        gigRepository.save(gig);
    }

    @Transactional
    public void becomeGig(BecomeGigRequestDTO dto) {
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();
//        Check user is verified or not
        if(!currentProfile.getIsVerified())
            throw new ForbiddenException("User is not verified ..");

//        Check ( ) -> Only User can become a gig
        if(currentProfile.getGig() != null)
            throw new ForbiddenException("Only User can become a gig..");

//        Check GIG Availability Status and Job Category
        AvailabilityStatus availabilityStatus;
        JobCategory jobCategory;

        try {
            availabilityStatus = AvailabilityStatus.valueOf(dto.getAvailabilityStatus().trim().toUpperCase());
        }catch (IllegalArgumentException e){
            throw new InvalidStatusException("Invalid Availability Status..");
        }

        try {
            jobCategory = JobCategory.valueOf(dto.getJobCategory().trim().toUpperCase());
        }catch (IllegalArgumentException e){
            throw new InvalidStatusException("Invalid Availability Status..");
        }

//        Check ( ) -> Skills mustn't null and empty
        if (dto.getSkillsId() == null || dto.getSkillsId().isEmpty())
            throw new BadRequestException("At least one skill is required");

//        Get distinct skills and get skills By ID
        List<Long> distinctSkillList = dto.getSkillsId().stream().distinct().toList();
        List<Skills> skills = skillsRepository.findAllById(distinctSkillList);

        if (skills.size() != distinctSkillList.size())
            throw new RuntimeException("One or more skills not found");

        GIG gig = GIG.builder()
                .user(currentProfile)
                .title(dto.getTitle())
                .jobCategory(jobCategory)
                .description(dto.getDescription())
                .availabilityStatus(availabilityStatus)
                .college(dto.getCollege())
                .department(dto.getDepartment())
                .semester(dto.getSemester())
                .build();

//        Set Skills and Save it in SB
        List<UserSkills> userSkills = skills.stream().map((skill -> new UserSkills(gig, skill))).toList();

        gig.setUserSkills(userSkills);
        currentProfile.getRoles().clear();
        currentProfile.getRoles().add(Roles.GIG);

        userEntityRepository.save(currentProfile);
        gigRepository.save(gig);

    }

//    Get GIG Profile
    public GigResponseDTO getMyGigProfile() {
//        Get Current Logged-in profile
        UserEntity currentProfile = authService.getCurrentProfile();

        // Check user is verified
        if (!currentProfile.getIsVerified())
            throw new ForbiddenException("User is not verified ..");

//        Check ( ) -> Current Profile is GIG or not ?
        if(currentProfile.getGig() == null)
            throw new ResourceNotFoundException("Gig profile not found ..");

        // Get current user's GIG profile
        GIG gig = currentProfile.getGig();

//        Convert GIG skills in Skill Response DTO
        List<SkillResponseDTO> skills = gig.getUserSkills().stream()
                .map(us -> SkillResponseDTO.builder()
                        .id(us.getSkill().getId())
                        .skill(us.getSkill().getSkill())
                        .build())
                .toList();

        return GigResponseDTO.builder()
                .id(gig.getId())
                .gigFirstName(currentProfile.getFirstName())
                .gigLastName(currentProfile.getLastName())
                .gigEmail(currentProfile.getEmail())
                .gigPhoneNumber(currentProfile.getPhoneNumber())
                .title(gig.getTitle())
                .jobCategory(gig.getJobCategory().name())
                .description(gig.getDescription())
                .availabilityStatus(gig.getAvailabilityStatus())
                .college(gig.getCollege())
                .department(gig.getDepartment())
                .semester(gig.getSemester())
                .gigSkills(skills)
                .build();
    }

//    Get GIG By ID
    public GigResponseDTO getGigProfileById(Long gigId) {
//        Get GIG by ID
        GIG gig = gigRepository.findById(gigId).orElseThrow(() -> new ResourceNotFoundException("Gig not found with ID: " + gigId));
        UserEntity user = gig.getUser();

//        Convert GIG skills in skill Respone DTO
        List<SkillResponseDTO> skills = gig.getUserSkills().stream()
                .map(us -> SkillResponseDTO.builder()
                        .id(us.getSkill().getId())
                        .skill(us.getSkill().getSkill())
                        .build())
                .toList();

        return GigResponseDTO.builder()
                .id(gig.getId())
                .gigFirstName(user.getFirstName())
                .gigLastName(user.getLastName())
//                .gigEmail(user.getEmail())
//                .gigPhoneNumber(user.getPhoneNumber())
                .title(gig.getTitle())
                .jobCategory(gig.getJobCategory().name())
                .description(gig.getDescription())
                .availabilityStatus(gig.getAvailabilityStatus())
                .college(gig.getCollege())
                .department(gig.getDepartment())
                .semester(gig.getSemester())
                .gigSkills(skills)
                .build();
    }

//    Edit GIG Profile
    @Transactional
    public void editGigProfile(BecomeGigRequestDTO dto) {
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

        // Check user is verified
        if (!currentProfile.getIsVerified()) {
            throw new ForbiddenException("User is not verified ..");
        }

        if (currentProfile.getGig() == null) {
            throw new ResourceNotFoundException("Gig profile not found ..");
        }

        // Get existing GIG profile
        GIG gig = currentProfile.getGig();

        // Update title
        if (dto.getTitle() != null)
            gig.setTitle(dto.getTitle());

        // Update Job Category
        if (dto.getJobCategory() != null) {
            try {
                JobCategory jobCategory = JobCategory.valueOf(dto.getJobCategory().trim().toUpperCase());
                gig.setJobCategory(jobCategory);
            } catch (IllegalArgumentException e) {
                throw new InvalidStatusException("Invalid Job Category..");
            }
        }

        // Update Availability Status
        if (dto.getAvailabilityStatus() != null) {
            try {
                AvailabilityStatus availabilityStatus =AvailabilityStatus.valueOf(dto.getAvailabilityStatus().trim().toUpperCase());
                gig.setAvailabilityStatus(availabilityStatus);
            } catch (IllegalArgumentException e) {
                throw new InvalidStatusException("Invalid Availability Status..");
            }
        }

        // Update Description
        if (dto.getDescription() != null)
            gig.setDescription(dto.getDescription());

        // Update College
        if (dto.getCollege() != null)
            gig.setCollege(dto.getCollege());

        // Update Department
        if (dto.getDepartment() != null)
            gig.setDepartment(dto.getDepartment());

        // Update Semester
        if (dto.getSemester() != null)
            gig.setSemester(dto.getSemester());

        // Save existing GIG
        gigRepository.save(gig);
    }

//    Job Proposal
    public void applyForJob(JobApplicationRequestDTO dto){
        UserEntity currentProfile = authService.getCurrentProfile();
        Roles roles = currentProfile.getRoles().iterator().next();

//        Check user is verified or not
        if(!currentProfile.getIsVerified()) throw new ForbiddenException("User is not verified ..");

        if(!roles.equals(Roles.GIG)) throw new ForbiddenException("Only gig can apply for Job ..");

        GIG gig = currentProfile.getGig();

//        Fetch Job by ID
        Long jobId = dto.getJobId();
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found.."));

//        Check Job Status Deadline and Bid Amount
        if (!job.getJobStatus().equals(JobStatus.OPEN))
            throw new InvalidStatusException("Job is : "+job.getJobStatus().name());

        if (dto.getDeliveryDate().isAfter(job.getDeadline()))
            throw new BadRequestException("Delivery date cannot be after the job deadline");

        if (dto.getBidAmount().compareTo(job.getBudget()) > 0)
            throw new BadRequestException("Bid amount cannot exceed the job budget");

        if (dto.getDeliveryDate().isBefore(LocalDate.now()))
            throw new BadRequestException("Delivery date cannot be in the past");

//        boolean existsByJobIdAndGigId = jobApplicationRepository.existsByJobIdAndGigId(jobId, gig.getId());
        Optional<JobApplication>existingApplication  = jobApplicationRepository.findByGigAndJob(gig.getId(), job.getId());
        if (existingApplication.isPresent()) {
            JobApplication application = existingApplication.get();

            if (application.getJobApplicationStatus() == JobApplicationStatus.APPLIED)
                throw new InvalidStatusException("You already applied for the job ..");

            if (application.getJobApplicationStatus() == JobApplicationStatus.REJECTED ||
                    application.getJobApplicationStatus() == JobApplicationStatus.SHORTLISTED ||
                    application.getJobApplicationStatus() == JobApplicationStatus.WITHDRAWN ||
                    application.getJobApplicationStatus() == JobApplicationStatus.ACCEPTED) {

                throw new InvalidStatusException("You cannot apply for job because your job application is already : " + application.getJobApplicationStatus());
            }
        }

//        Save Job Proposal in DB
        JobApplication newJobApplication = JobApplication.builder()
                .coverLetter(dto.getCoverLetter())
                .bidAmount(dto.getBidAmount())
                .job(job)
                .jobApplicationStatus(JobApplicationStatus.APPLIED)
                .deliveryDate(dto.getDeliveryDate())
                .gig(gig)
                .build();

        jobApplicationRepository.save(newJobApplication);
        notificationService.notify(
                job.getClient(),
                NotificationType.NEW_PROPOSAL,
                "New Job Proposal",
                currentProfile.getFirstName() + " has submitted a proposal for \"" + job.getTitle() + "\".",
                newJobApplication.getId()
        );
    }

//    Withdraw Job Proposal By Job ID
    public void withdrawJobApplicationByJobId(Long jobId){
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found with ID : " + jobId + ".."));
        UserEntity currentProfile = authService.getCurrentProfile();
        GIG gig = currentProfile.getGig();

//        Check user is verified or not
        if(!currentProfile.getIsVerified())
            throw new RuntimeException("User is not verified ..");

        Roles roles = currentProfile.getRoles().iterator().next();

        if(!roles.equals(Roles.GIG))
            throw new RuntimeException("Only Gig can Withdraw job..");

//        Fetch Job Proposal by job ID , GIG ID and Job Application Status
        JobApplication jobApplication = jobApplicationRepository.findByGigAndJobAndJobApplicationStatus(gig.getId(),job.getId(),JobApplicationStatus.APPLIED)
                .orElseThrow(() -> new ResourceNotFoundException("Job Application not found by Job Id or gig ID.."));

//        Check ( ) -> Proposal Authorization
        if (jobApplication.getGig() == null || !gig.getId().equals(jobApplication.getGig().getId()))
            throw new ForbiddenException("You are not authorized GIG to withdraw job application ..");

        if (jobApplication.getJobApplicationStatus() == JobApplicationStatus.WITHDRAWN)
            throw new InvalidStatusException("Job Application is already withdrawn ..");

        if(jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.ACCEPTED)
                || jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.REJECTED)
                || jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.SHORTLISTED))
            throw new InvalidStatusException("Cannot withdraw application because Application is "+jobApplication.getJobApplicationStatus().name());

//        Save Application in DB
        jobApplication.setJobApplicationStatus(JobApplicationStatus.WITHDRAWN);
        jobApplicationRepository.save(jobApplication);
        notificationService.notify(
                job.getClient(),
                NotificationType.CONTRACT_CANCELLED,
                "Proposal Withdrawn",
                gig.getUser().getFirstName() + " has withdrawn their proposal for \"" + job.getTitle() + "\".",
                jobApplication.getId()
        );
    }

//    Update Job Proposal
    public void updateJobApplication(Long jobApplicationId, UpdateJobApplicationRequestDTO dto) {
//        Get Job Application by job Application id
        JobApplication jobApplication = jobApplicationRepository.findById(jobApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Application not found by ID : "+jobApplicationId));

//        Get current logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();
        Roles roles = currentProfile.getRoles().iterator().next();

//        Check user is verified or not
        if(!currentProfile.getIsVerified())
            throw new ForbiddenException("User is not verified ..");

//        Check Profile Role only GIG can update Job Proposal
        if(!roles.equals(Roles.GIG))
            throw new ForbiddenException("Only GIG can Update there Job Application : ");

        GIG gig = currentProfile.getGig();

        if(jobApplication.getGig() == null || !jobApplication.getGig().getId().equals(gig.getId()))
            throw new ForbiddenException("You are not authorized to update Job Application ..\n");

        if(jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.WITHDRAWN) ||
                jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.REJECTED) ||
                jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.ACCEPTED) ||
                jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.SHORTLISTED)) {
            throw new InvalidStatusException("You can't update your job application because your job application is : "+jobApplication.getJobApplicationStatus().name());
        }

        Job job = jobApplication.getJob();

//        Validate bid amount
        if (dto.getBidAmount() == null || dto.getBidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Bid amount must be greater than zero");
        }

//        Bid amount should not be greater than the job budget
        if (dto.getBidAmount().compareTo(job.getBudget()) > 0) {
            throw new BadRequestException("Bid amount cannot be greater than the job budget");
        }

//        Validate delivery date
        if (dto.getDeliveryDate() == null) {
            throw new BadRequestException("Delivery date is required");
        }

//        Delivery date should not be in the past
        if (dto.getDeliveryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Delivery date cannot be in the past");
        }

//        Delivery date should not be after the expected delivery date
        if (dto.getDeliveryDate().isAfter(job.getDeadline()))
            throw new BadRequestException("Delivery date cannot be after the expected delivery date");

        if(dto.getCoverLetter() != null)
            jobApplication.setCoverLetter(dto.getCoverLetter());

        jobApplicationRepository.save(jobApplication);
    }

//    Withdrawn Job Application by Job ID
    public void withdrawJobApplicationByJobApplicationId(Long jobApplicationId){
//        Get Job Application by JobApplication ID
        JobApplication jobApplication = jobApplicationRepository.findById(jobApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Application not found ..."));

//        Get Current Logged-in Prrofile
        UserEntity currentProfile = authService.getCurrentProfile();
        Roles roles = currentProfile.getRoles().iterator().next();

//        Check user is verified or not
        if(!currentProfile.getIsVerified())
            throw new ForbiddenException("User is not verified ..");

        if (!roles.equals(Roles.GIG))
            throw new RuntimeException("Only GIG can Withdraw application"+currentProfile.getEmail());

        GIG gig = currentProfile.getGig();

        if (jobApplication.getGig() == null || !gig.getId().equals(jobApplication.getGig().getId()))
            throw new ForbiddenException("You are not authorized GIG to withdraw job application ..");

        if (jobApplication.getJobApplicationStatus() == JobApplicationStatus.WITHDRAWN)
            throw new InvalidStatusException("Job Application is already withdrawn ..");

       if(!jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.APPLIED))
           throw new BadRequestException("You can't withdrawn your Job Application , Application is already : "+jobApplication.getJobApplicationStatus());

        jobApplication.setJobApplicationStatus(JobApplicationStatus.WITHDRAWN);
        jobApplicationRepository.save(jobApplication);
        notificationService.notify(
                jobApplication.getJob().getClient(),
                NotificationType.CONTRACT_CANCELLED,
                "Proposal Withdrawn",
                currentProfile.getFirstName() + " has withdrawn their proposal for \""
                        + jobApplication.getJob().getTitle() + "\".",
                jobApplication.getId()
        );
    }

//    Get All Job Application
    public List<JobApplicationSortingAndFilteringResponseDTO> getAllJobApplication(JobApplicationFilterAndSortingRequestDTO dto, Pageable pageable){
//        Get current logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();
        Roles roles = currentProfile.getRoles().iterator().next();

        if(!roles.equals(Roles.GIG)){
            throw new ForbiddenException("Only GIG can see their Job Application..");
        }
        if(!currentProfile.getIsVerified()){
            throw new ForbiddenException("User is GIG is not varified ..");
        }

        GIG gig = currentProfile.getGig();

        JobApplicationStatus status;
        try {
            status = JobApplicationStatus.valueOf(dto.getApplicationStatus().trim().toUpperCase());
        }catch (Exception e){
            throw new InvalidStatusException("Invalid Job Application Status : "+dto.getApplicationStatus());
        }

        Specification<JobApplication> specification = Specification.where(GigSpecification.hasGig(gig.getId()))
                .and(GigSpecification.budgetGraterThan(dto.getMinBidAmount()))
                .and(GigSpecification.budgetLessThan(dto.getMaxBidAmount()))
                .and(GigSpecification.hasStatus(status));

        return jobApplicationRepository.findAll(specification,pageable).stream().map(this::jobApplicationSortingAndFilteringResponseDTO).toList();
    }

//    helper methods
    private JobApplicationSortingAndFilteringResponseDTO jobApplicationSortingAndFilteringResponseDTO(JobApplication jobApplication){
        return JobApplicationSortingAndFilteringResponseDTO.builder()
                .budget(jobApplication.getBidAmount())
                .applyAt(jobApplication.getCreatedAt())
                .jobApplicationStatus(jobApplication.getJobApplicationStatus())
                .coverLetter(jobApplication.getCoverLetter())
                .deliveryDate(jobApplication.getDeliveryDate())
                .build();
    }
}
