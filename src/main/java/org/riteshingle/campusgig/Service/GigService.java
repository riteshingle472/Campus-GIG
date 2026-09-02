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
import org.riteshingle.campusgig.ResponseDTO.JobApplicationSortingAndFilteringResponseDTO;
import org.riteshingle.campusgig.Specification.GigSpecification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final JobRepository jobRepository;

//    Add User Skills
    @Transactional
    public void addSkills(List<Long> skillIds) {
//        Get current profile
        UserEntity currentProfile = authService.getCurrentProfile();
        Roles roles = currentProfile.getRoles().iterator().next();

//        Check user is verified or not
        if(!currentProfile.getIsVerified())
            throw new ForbiddenException("User is not verified ..");

//        Check gig
        if(!roles.equals(Roles.GIG)) throw new ForbiddenException("Only gig can add skills ..");

        GIG gig = currentProfile.getGig();

//        Get GIG existing skills
        List<Long> userExistingSkills = userSkillsRepository.findSkillIdsByGigId(gig.getId());
//        Filter skills from existing skills
        List<Long> newSkills = skillIds.stream().distinct().filter(id -> !userExistingSkills.contains(id)).toList();
//        Find Skills by ID in List
        List<Skills> skills = skillsRepository.findAllById(newSkills);

//        If List is empty then do nothing
        if (newSkills.isEmpty()) return;

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
        System.out.println("BEFORE GET PROFILE");

        UserEntity currentProfile = authService.getCurrentProfile();

        System.out.println("AFTER GET PROFILE");
        System.out.println("============================================================================");
//        Check user is verified or not
        if(!currentProfile.getIsVerified())
            throw new ForbiddenException("User is not verified ..");

        if(currentProfile.getGig() != null){
            throw new ForbiddenException("Only User can become a gig..");
        }

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

        if (dto.getSkillsId() == null || dto.getSkillsId().isEmpty())
            throw new BadRequestException("At least one skill is required");

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

        List<UserSkills> userSkills = skills.stream().map((skill -> new UserSkills(gig, skill))).toList();

        gig.setUserSkills(userSkills);
        currentProfile.getRoles().clear();
        currentProfile.getRoles().add(Roles.GIG);

        userEntityRepository.save(currentProfile);
        gigRepository.save(gig);

    }

    public void applyForJob(JobApplicationRequestDTO dto){
        UserEntity currentProfile = authService.getCurrentProfile();
        Roles roles = currentProfile.getRoles().iterator().next();

//        Check user is verified or not
        if(!currentProfile.getIsVerified()) throw new ForbiddenException("User is not verified ..");

        if(!roles.equals(Roles.GIG)) throw new ForbiddenException("Only gig can apply for Job ..");

        GIG gig = currentProfile.getGig();

        Long jobId = dto.getJobId();
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found.."));

        if (!job.getJobStatus().equals(JobStatus.OPEN))
            throw new InvalidStatusException("Job is : "+job.getJobStatus().name());

        if (dto.getDeliveryDate().isAfter(job.getDeadline()))
            throw new BadRequestException("Delivery date cannot be after the job deadline");

        if (dto.getBidAmount().compareTo(job.getBudget()) > 0)
            throw new BadRequestException("Bid amount cannot exceed the job budget");

        if (dto.getDeliveryDate().isBefore(LocalDate.now()))
            throw new BadRequestException("Delivery date cannot be in the past");

//        boolean existsByJobIdAndGigId = jobApplicationRepository.existsByJobIdAndGigId(jobId, gig.getId());
        Optional<JobApplication> jobApplicationByJobIdAndGigIdAndJobApplicationStatus = jobApplicationRepository.findByGigAndJobAndJobApplicationStatus(gig, job, JobApplicationStatus.APPLIED);
        JobApplication jobApplication = null;

        if(jobApplicationByJobIdAndGigIdAndJobApplicationStatus.isPresent()){
            jobApplication = jobApplicationByJobIdAndGigIdAndJobApplicationStatus.get();
        }

       if(jobApplication != null){
           if(jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.APPLIED))
               throw new InvalidStatusException("You already applied for the job ..");

           if(jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.REJECTED) ||
                   jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.SHORTLISTED) ||
                   jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.ACCEPTED)){
               throw new InvalidStatusException("You cannot apply for job because your job application is : "+jobApplication.getJobApplicationStatus());
           }
       }

        JobApplication newJobApplication = JobApplication.builder()
                .coverLetter(dto.getCoverLetter())
                .bidAmount(dto.getBidAmount())
                .job(job)
                .jobApplicationStatus(JobApplicationStatus.APPLIED)
                .deliveryDate(dto.getDeliveryDate())
                .gig(gig)
                .build();

        jobApplicationRepository.save(newJobApplication);
    }
    
    public void withdrawJobApplicationByJobId(Long jobId){
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found with ID : " + jobId + ".."));
        UserEntity client = authService.getCurrentProfile();
        GIG gig = client.getGig();

//        Check user is verified or not
        if(!client.getIsVerified())
            throw new RuntimeException("User is not verified ..");

        if(gig == null)
            throw new RuntimeException("Only Gig can Withdraw job..");

        JobApplication jobApplication = jobApplicationRepository.findByGigAndJobAndJobApplicationStatus(gig,job,JobApplicationStatus.APPLIED)
                .orElseThrow(() -> new ResourceNotFoundException("Job Application not found by Job Id or gig ID.."));

        if (jobApplication.getGig() == null || !gig.getId().equals(jobApplication.getGig().getId()))
            throw new ForbiddenException("You are not authorized GIG to withdraw job application ..");

        if (jobApplication.getJobApplicationStatus() == JobApplicationStatus.WITHDRAWN)
            throw new InvalidStatusException("Job Application is already withdrawn ..");

        if(jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.ACCEPTED)
                || jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.REJECTED)
                || jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.SHORTLISTED))
            throw new InvalidStatusException("Cannot withdraw application because Application is "+jobApplication.getJobApplicationStatus().name());


        jobApplication.setJobApplicationStatus(JobApplicationStatus.WITHDRAWN);
        jobApplicationRepository.save(jobApplication);

    }

    public void updateJobApplication(Long jobApplicationId, UpdateJobApplicationRequestDTO dto) {
        JobApplication jobApplication = jobApplicationRepository.findById(jobApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Application not found by ID : "+jobApplicationId));

        UserEntity currentProfile = authService.getCurrentProfile();
        Roles roles = currentProfile.getRoles().iterator().next();

//        Check user is verified or not
        if(!currentProfile.getIsVerified())
            throw new ForbiddenException("User is not verified ..");

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

        if(dto.getBidAmount() != null)
            jobApplication.setBidAmount(dto.getBidAmount());

        if(dto.getDeliveryDate() != null)
            jobApplication.setDeliveryDate(dto.getDeliveryDate());

        if(dto.getCoverLetter() != null)
            jobApplication.setCoverLetter(dto.getCoverLetter());

        jobApplicationRepository.save(jobApplication);
    }

    public void withdrawJobApplicationByJobApplicationId(Long jobApplicationId){
        JobApplication jobApplication = jobApplicationRepository.findById(jobApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Application not found ..."));

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

        if(jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.ACCEPTED)
                || jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.REJECTED)
                || jobApplication.getJobApplicationStatus().equals(JobApplicationStatus.SHORTLISTED))
            throw new InvalidStatusException("Cannot withdraw application because Application is "+jobApplication.getJobApplicationStatus().name());

        jobApplication.setJobApplicationStatus(JobApplicationStatus.WITHDRAWN);
        jobApplicationRepository.save(jobApplication);

    }

    public List<JobApplicationSortingAndFilteringResponseDTO> getAllJobApplication(JobApplicationFilterAndSortingRequestDTO dto, Pageable pageable){
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
