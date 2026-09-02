package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.JobStatus;
import org.riteshingle.campusgig.Enum.Roles;
import org.riteshingle.campusgig.Exception.BadRequestException;
import org.riteshingle.campusgig.Exception.ForbiddenException;
import org.riteshingle.campusgig.Exception.ResourceNotFoundException;
import org.riteshingle.campusgig.Model.GIG;
import org.riteshingle.campusgig.Model.Job;
import org.riteshingle.campusgig.Model.Bookmark;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.Repository.JobRepository;
import org.riteshingle.campusgig.Repository.SaveJobRepository;
import org.riteshingle.campusgig.ResponseDTO.BookmarkResponseDTO;
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

    public void bookmarkJob(Long jobId){
        UserEntity currentProfile = authService.getCurrentProfile();

        Roles roles = currentProfile.getRoles().iterator().next();
        if(roles.equals(Roles.USER)){
            throw new ForbiddenException("Only GIG and Client can save Job..");
        }

        if(!currentProfile.getIsVerified()){
            throw new ForbiddenException("GIG is not Verified");
        }

        GIG gig = currentProfile.getGig();

        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found with job id : "+jobId));

        if(saveJobRepository.existsByGigIdAndJobId(gig.getId(), jobId)) throw new ResourceNotFoundException("Job already saved");
        if(job.getDeadline().isBefore(LocalDate.now())) throw new BadRequestException("Cannot save expired job");
        if(!job.getJobStatus().equals(JobStatus.OPEN)) throw new BadRequestException("Cannot save job , job is : "+job.getJobStatus().name());

        Bookmark build = Bookmark.builder()
                .gig(gig)
                .job(job)
                .build();

        saveJobRepository.save(build);
    }

    public void removeBookmarkJob(Long jobId){
        UserEntity currentProfile = authService.getCurrentProfile();
        Roles roles = currentProfile.getRoles().iterator().next();

        if(roles.equals(Roles.USER)){
            throw new ForbiddenException("Only GIG and Client can save Job..");
        }
        if(!currentProfile.getIsVerified())
            throw new ForbiddenException("GIG is not Verified");

        GIG gig = currentProfile.getGig();
        Bookmark bookmark = saveJobRepository.findByJobIdAndGigId(jobId, gig.getId()).orElseThrow(() -> new ForbiddenException("You are not authorized to remove job from saves.."));

        saveJobRepository.delete(bookmark);
    }

    public List<BookmarkResponseDTO> bookmarkJobs() {
        UserEntity currentProfile = authService.getCurrentProfile();
        Roles roles = currentProfile.getRoles().iterator().next();
        GIG gig = currentProfile.getGig();

        if(roles.equals(Roles.USER)){
            throw new ForbiddenException("Only GIG and Client can save Job..");
        }
        if (!currentProfile.getIsVerified())
            throw new ForbiddenException("GIG is not Verified");

        List<Bookmark> byGigId = saveJobRepository.findByGigId(gig.getId());
        return byGigId.stream().map(this::responseDTO).toList();
    }

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
