package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.AvailabilityStatus;
import org.riteshingle.campusgig.Enum.JobStatus;
import org.riteshingle.campusgig.Model.GIG;
import org.riteshingle.campusgig.Model.Job;
import org.riteshingle.campusgig.Model.Bookmark;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.Repository.GigRepository;
import org.riteshingle.campusgig.Repository.JobRepository;
import org.riteshingle.campusgig.Repository.SaveJobRepository;
import org.riteshingle.campusgig.ResponseDTO.BookmarkResponseDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final SaveJobRepository saveJobRepository;
    private final AuthService authService;
    private final JobRepository jobRepository;
    private final GigRepository gigRepository;


    public void saveJob(Long jobId){
        UserEntity currentProfile = authService.getCurrentProfile();

        if(currentProfile.getGig() == null){
            throw new RuntimeException("Only GIG can save Job..");
        }

        if(!currentProfile.getIsVerified()){
            throw new RuntimeException("GIG is not Verified");
        }

        GIG gig = currentProfile.getGig();

        if(gig.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE)
            throw new RuntimeException("GIG is currently unavailable");

        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found with job id : "+jobId));

        if(saveJobRepository.existsByGigIdAndJobId(gig.getId(), jobId)) throw new RuntimeException("Job already saved");
        if(job.getDeadline().isBefore(LocalDate.now())) throw new RuntimeException("Cannot save expired job");
        if(!job.getJobStatus().equals(JobStatus.OPEN)) throw new RuntimeException("Cannot save job , job is : "+job.getJobStatus().name());

        Bookmark build = Bookmark.builder()
                .gig(gig)
                .job(job)
                .build();

        saveJobRepository.save(build);
    }

    public void removeJobFromSave(Long jobId){
        UserEntity currentProfile = authService.getCurrentProfile();

        if(currentProfile.getGig() == null)
            throw new RuntimeException("Only GIG can save Job..");

        if(!currentProfile.getIsVerified())
            throw new RuntimeException("GIG is not Verified");

        GIG gig = currentProfile.getGig();
        Bookmark bookmark = saveJobRepository.findByJobIdAndGigId(jobId, gig.getId()).orElseThrow(() -> new RuntimeException("You are not authorized to remove job from saves.."));

        saveJobRepository.delete(bookmark);
    }

    public List<BookmarkResponseDTO> getSaveJob() {
        UserEntity currentProfile = authService.getCurrentProfile();
        GIG gig = currentProfile.getGig();


        if (currentProfile.getGig() == null)
            throw new RuntimeException("Only GIG can save Job..");

        if (!currentProfile.getIsVerified())
            throw new RuntimeException("GIG is not Verified");

        List<Bookmark> byGigId = saveJobRepository.findByGigId(gig.getId());
        return byGigId.stream().map(this::responseDTO).toList();
    }

    private BookmarkResponseDTO responseDTO(Bookmark bookmark){
        Job job = bookmark.getJob();
        return BookmarkResponseDTO.builder()
                .workMode(job.getWorkMode())
                .jobStatus(job.getJobStatus())
                .deadline(job.getDeadline())
                .jobTitle(job.getTitle())
                .experienceLevel(job.getExperienceLevel())
                .budget(job.getBudget())
                .category(job.getCategory())
                .build();
    }
}
