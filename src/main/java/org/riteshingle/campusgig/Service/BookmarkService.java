package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.JobStatus;
import org.riteshingle.campusgig.Enum.Roles;
import org.riteshingle.campusgig.Exception.BadRequestException;
import org.riteshingle.campusgig.Exception.ConflictException;
import org.riteshingle.campusgig.Exception.ForbiddenException;
import org.riteshingle.campusgig.Exception.ResourceNotFoundException;
import org.riteshingle.campusgig.Model.GIG;
import org.riteshingle.campusgig.Model.Job;
import org.riteshingle.campusgig.Model.Bookmark;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.Repository.JobRepository;
import org.riteshingle.campusgig.Repository.SaveJobRepository;
import org.riteshingle.campusgig.ResponseDTO.BookmarkResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {
    private final SaveJobRepository saveJobRepository;
    private final AuthService authService;
    private final JobRepository jobRepository;

//    Bookmark Job
    public void bookmarkJob(Long jobId){
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

//        Get User Role
        Roles roles = currentProfile.getRoles().iterator().next();

//        Only GIG and Client can save Job
        if(!roles.equals(Roles.GIG))
            throw new ForbiddenException("Only GIG can save Job..");

//        Chek user is verified or not
        if(!currentProfile.getIsVerified()){
            throw new ForbiddenException("GIG is not Verified");
        }

        if(currentProfile.getGig() == null)
            throw new BadRequestException("Only GIG can save Job");

//        Get GIG
        GIG gig = currentProfile.getGig();

//        Get Job By ID
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found with job id : "+jobId));

//        Check Job is already saved By GIG
        if(saveJobRepository.existsByGigIdAndJobId(gig.getId(), jobId)) throw new ConflictException("Job already saved");
//        Check Job Deadline ,You can't save Expire Job
        if(job.getDeadline().isBefore(LocalDate.now())) throw new BadRequestException("Cannot save expired job");
//        Check Job status
        if(!job.getJobStatus().equals(JobStatus.OPEN)) throw new BadRequestException("Cannot save job , job is : "+job.getJobStatus().name());

//        Save Bookmark in DB
        Bookmark build = Bookmark.builder()
                .gig(gig)
                .job(job)
                .build();

        saveJobRepository.save(build);
    }

//    Remove Bookmark
    public void removeBookmarkJob(Long jobId){
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();
//        Get GIG Role
        Roles roles = currentProfile.getRoles().iterator().next();

//        Check ( ) -> Only GIG can remove Bookmark
        if(!roles.equals(Roles.GIG))
            throw new ForbiddenException("Only GIG and Client can save Job..");

//        Check Profile is verified
        if(!currentProfile.getIsVerified())
            throw new ForbiddenException("GIG is not Verified");

//        Get GIG profile
        GIG gig = currentProfile.getGig();
//        fetch Bookmark By GIG and Job ID
        Bookmark bookmark = saveJobRepository.findByJobIdAndGigId(jobId, gig.getId()).orElseThrow(() -> new ForbiddenException("You are not authorized to remove job from saves.."));
//        Remove Bookmark permanent
        saveJobRepository.delete(bookmark);
    }

//    Get Bookmark
    public List<BookmarkResponseDTO> bookmarkJobs(Pageable pageable) {
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();
//        Get GIG Role
        Roles roles = currentProfile.getRoles().iterator().next();
//        Get GIG
        GIG gig = currentProfile.getGig();

//        Check ( ) -> Only GIG can See Bookmark
        if(!roles.equals(Roles.GIG))
            throw new ForbiddenException("Only GIG and Client can save Job..");

//        Check GIG is verified
        if (!currentProfile.getIsVerified())
            throw new ForbiddenException("GIG is not Verified");

//        Get Bookmark By GIG ID
        List<Bookmark> byGigId = saveJobRepository.findByGigId(gig.getId(),pageable).getContent();
        return byGigId.stream().map(this::responseDTO).toList();
    }

//    Helper method
    private BookmarkResponseDTO responseDTO(Bookmark bookmark){
        Job job = bookmark.getJob();
        return BookmarkResponseDTO.builder()
                .jobStatus(job.getJobStatus())
                .deadline(job.getDeadline())
                .jobTitle(job.getTitle())
                .experienceLevel(job.getExperienceLevel())
                .budget(job.getBudget())
                .category(job.getCategory())
                .build();
    }
}
