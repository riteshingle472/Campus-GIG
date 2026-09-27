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

        if(!currentProfile.getIsVerified())
            throw new ForbiddenException("GIG is not Verified");

//        Check Current Profile Role
//        Only GIG can Bookmark Job
        if(currentProfile.getRoles().contains(Roles.GIG))
            throw new ForbiddenException("You must complete your Gig profile to save jobs");

        GIG gig = currentProfile.getGig();

        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found with job id : "+jobId));

//        Check Job is already bookmarked or not
        if(saveJobRepository.existsByGigIdAndJobId(gig.getId(), jobId)) throw new ConflictException("Job already saved");
//        Can't Bookmark expire Job
        if(job.getDeadline().isBefore(LocalDate.now())) throw new BadRequestException("Cannot save expired job");
//        Only Open Job you can bookmark
        if(!job.getJobStatus().equals(JobStatus.OPEN)) throw new BadRequestException("Cannot save job , job is : "+job.getJobStatus().name());

//        Create Bookmark and save in DB
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

//        Check Profile is verified ?
        if(!currentProfile.getIsVerified())
            throw new ForbiddenException("GIG is not Verified");

//        Check Current Profile Role
//        Only GIG can Remove Bookmark
        if(currentProfile.getRoles().contains(Roles.GIG))
            throw new ForbiddenException("You must complete your Gig profile to save jobs");

//        Get GIG from Current Profile
        GIG gig = currentProfile.getGig();

//        Get Bookmark by Job and GIG id
        Bookmark bookmark = saveJobRepository.findByJobIdAndGigId(jobId, gig.getId()).orElseThrow(() -> new ForbiddenException("You are not authorized to remove job from saves.."));
        saveJobRepository.delete(bookmark);
    }

//    Get Bookmarks
    public List<BookmarkResponseDTO> bookmarkJobs(Pageable pageable) {
//        Get Current Logged-in Profile
        UserEntity currentProfile = authService.getCurrentProfile();

//        Check Profile is verified ?
        if(!currentProfile.getIsVerified())
            throw new ForbiddenException("GIG is not Verified");

//        Check Current Profile Role
//        Only GIG can Remove Bookmark
        if(currentProfile.getRoles().contains(Roles.GIG))
            throw new ForbiddenException("You must complete your Gig profile to save jobs");

//        Get GIG from Current Profile
        GIG gig = currentProfile.getGig();

//        Fetch Bookmarks by GIG id
        List<Bookmark> byGigId = saveJobRepository.findByGigId(gig.getId(),pageable).getContent();
        return byGigId.stream().map(this::responseDTO).toList();
    }

//    Helper Methods
    private BookmarkResponseDTO responseDTO(Bookmark bookmark){
        Job job = bookmark.getJob();
        return BookmarkResponseDTO.builder()
                .jobId(job.getId())
                .jobStatus(job.getJobStatus())
                .deadline(job.getDeadline())
                .jobTitle(job.getTitle())
                .experienceLevel(job.getExperienceLevel())
                .budget(job.getBudget())
                .category(job.getCategory())
                .build();
    }
}