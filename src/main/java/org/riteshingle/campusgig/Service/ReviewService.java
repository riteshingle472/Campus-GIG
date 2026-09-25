package org.riteshingle.campusgig.Service;

import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Enum.ContractStatus;
import org.riteshingle.campusgig.Exception.*;
import org.riteshingle.campusgig.Model.Contract;
import org.riteshingle.campusgig.Model.Review;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.Repository.ContractRepository;
import org.riteshingle.campusgig.Repository.ReviewRepository;
import org.riteshingle.campusgig.Repository.UserEntityRepository;
import org.riteshingle.campusgig.RequestDTO.CreateReviewRequest;
import org.riteshingle.campusgig.ResponseDTO.ReviewResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {
    private final AuthService authService;
    private final ReviewRepository reviewRepository;
    private final ContractRepository contractRepository;
    private final UserEntityRepository userEntityRepository;
    private final NotificationService notificationService;

//    Create Review
    public void postReview(Long contractId, CreateReviewRequest dto){
//        Current Logged-in user is reviewer
        UserEntity reviewer = authService.getCurrentProfile();
//        Get contract by id
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not fond by ID : "+contractId));

//        Check ( ) -> Contract must Complete
        if(!contract.getContractStatus().equals(ContractStatus.COMPLETE))
            throw new InvalidStatusException("Review can only be given after contract is closed");

        UserEntity reviewee;

        if (contract.getClient().getId().equals(reviewer.getId()))
            reviewee = contract.getGig().getUser();
        else if(contract.getGig().getUser().getId().equals(reviewer.getId()))
            reviewee = contract.getClient();
        else throw new ForbiddenException("You are not a participant of this contract");

//        Check ( ) -> Reviewer is already given review
        boolean alreadyReviewed = reviewRepository.existsByContractIdAndReviewerId(contractId,reviewer.getId());

        if (alreadyReviewed)
            throw new ConflictException("You had reviewed this contract");

//        Check ( ) -> rating must be grater than 0 and less than 6
        if (dto.getRating() == null || dto.getRating() < 0 || dto.getRating() > 5)
            throw new BadRequestException("Rating must be between 0 to 5 ..");

//        Save Review in DB
        Review review = Review.builder()
                .rating(dto.getRating())
                .comment(dto.getComment())
                .contract(contract)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .build();

        review = reviewRepository.save(review);
        adjustRating(reviewee, review.getRating(), 1);

        notificationService.notify(
                reviewee,
                org.riteshingle.campusgig.Enum.NotificationType.NEW_REVIEW,
                "New Review Received",
                reviewer.getFirstName() + " has given you a " + review.getRating() + "-star review.",
                contractId
        );
    }

//    Delete Review
    public void deleteReview(Long contractId){
//        Get current Logged-in Reviewer
        UserEntity reviewer = authService.getCurrentProfile();
//        fetch Contract by contract Id
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("Contract not fond by ID : "+contractId));

//        Check ( ) -> Only Contract gig and client can delete their review
        if(!reviewer.getId().equals(contract.getGig().getUser().getId()) ||
            !reviewer.getId().equals(contract.getClient().getId())){
            throw new ForbiddenException("You aren't participant of this contract ID : "+contractId);
        }
        Review review = reviewRepository.findByContractIdAndReviewerId(contractId, reviewer.getId()).orElseThrow(() -> new ResourceNotFoundException("Review not found .."));

//        Check ( ) -> You can delete review after completing contract
        if(!contract.getContractStatus().equals(ContractStatus.COMPLETE))
            throw new InvalidStatusException("Review can only be delete after contract is closed");

        adjustRating(review.getReviewee(), -review.getRating(), -1);
        reviewRepository.delete(review);
    }

//    Update Review
    public void updateReview(Long contractId,CreateReviewRequest dto){
//        Get Current Logged-in Reviewer
        UserEntity reviewer = authService.getCurrentProfile();
//        Get contract by contract Id
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("Contract not fond.."));

//        Check ( ) -> You can Update Review after completing Contract
        if (!contract.getContractStatus().equals(ContractStatus.COMPLETE))
            throw new InvalidStatusException("Review can only be updated after contract is closed");

//        Get Review by  reviewer and contract id
        Review review = reviewRepository.findByContractIdAndReviewerId(contractId, reviewer.getId()).orElseThrow(() -> new ResourceNotFoundException("Review not found .."));

        if(dto.getComment() != null && !dto.getComment().isBlank())
            review.setComment(dto.getComment());

//        Check ( ) ->  rating mustn't null
        if (dto.getRating() != null) {
//            check ( ) -> Rating must be grater than 0 and less than 6
            if (dto.getRating() < 0 || dto.getRating() > 6)
                throw new BadRequestException("Rating must be between 0 to 5");

//            Get Old Rating
            Integer oldRating = review.getRating();
            if (!dto.getRating().equals(oldRating)) {
                review.setRating(dto.getRating());
                adjustRating(review.getReviewee(), dto.getRating() - oldRating, 0);
            }

            reviewRepository.save(review);
        }
    }

//    Get All reviews
    public List<ReviewResponseDTO> reviews(Pageable pageable){
//        Get Current Logged-in User
        UserEntity user = authService.getCurrentProfile();
//        Fetch all reviews
        List<Review> reviewList = reviewRepository.findByRevieweeId(user.getId(),pageable).getContent();

        return reviewList.stream().map(review -> ReviewResponseDTO.builder()
                .name(review.getReviewer().getFirstName()+" "+review.getReviewer().getLastName())
                .createdAt(review.getCreatedAt())
                .comment(review.getComment() == null ? "" : review.getComment())
                .rating(review.getRating())
                .build()
        ).toList();
    }

    public Optional<ReviewResponseDTO> getMyReview(Long contractId) {
        UserEntity reviewer = authService.getCurrentProfile();

        return reviewRepository.findByContractIdAndReviewerId(contractId, reviewer.getId())
                .map(review -> ReviewResponseDTO.builder()
                        .name(review.getReviewer().getFirstName() + " " + review.getReviewer().getLastName())
                        .createdAt(review.getCreatedAt())
                        .comment(review.getComment() == null ? "" : review.getComment())
                        .rating(review.getRating())
                        .build());
    }

//    Adjust Rating
    private void adjustRating(UserEntity reviewee, long ratingDelta, long countDelta) {
//        Get review sum and count
        long sum = reviewee.getTotalRatingSum() == null ? 0L : reviewee.getTotalRatingSum();
        long count = reviewee.getTotalRatings() == null ? 0L : reviewee.getTotalRatings();

//        Add delta rating sum
        reviewee.setTotalRatingSum(sum + ratingDelta);
        reviewee.setTotalRatings(count + countDelta);

//        save in DB
        userEntityRepository.save(reviewee);
    }
}
