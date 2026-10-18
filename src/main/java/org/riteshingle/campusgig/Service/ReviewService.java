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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ReviewRepository reviewRepository;
    private final ContractRepository contractRepository;
    private final UserEntityRepository userEntityRepository;
    private final NotificationService notificationService;

//    Review
    @CachePut(value = "Review",key = "#contractId + ':id:' + #reviewerId")
    public ReviewResponseDTO postReview(Long contractId,Long reviewerId,UserEntity reviewer ,CreateReviewRequest dto){
//        Get Contract by ID
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not fond by ID : "+contractId));

//        Check Review is already given or not
        boolean alreadyReviewed = reviewRepository.existsByContractIdAndReviewerId(contractId,reviewerId);

//        Review can post only after contract completing
        if(!contract.getContractStatus().equals(ContractStatus.COMPLETE))
            throw new InvalidStatusException("Review can only be given after contract is closed");

//        Reviewee
        UserEntity reviewee;
        if (contract.getClient().getId().equals(reviewerId))
            reviewee = contract.getGig().getUser();
        else if(contract.getGig().getUser().getId().equals(reviewerId))
            reviewee = contract.getClient();
        else throw new ForbiddenException("You are not a participant of this contract");

//        Conflict Exception
//        One Reviewer can post only one review to reviewee
        if (alreadyReviewed)
            throw new ConflictException("You have already reviewed this contract");

//        Rating must between 1 to 5
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5)
            throw new BadRequestException("Rating must be between 1 and 5");

//        Create Review and sent in DB
        Review review = Review.builder()
                .rating(dto.getRating())
                .comment(dto.getComment())
                .contract(contract)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .build();

        review = reviewRepository.save(review);
//        Adjust Rating
        adjustRating(reviewee, review.getRating(), 1);

        notificationService.notify(
                reviewee,
                org.riteshingle.campusgig.Enum.NotificationType.NEW_REVIEW,
                "New Review Received",
                reviewer.getFirstName() + " has given you a " + review.getRating() + "-star review.",
                contractId
        );

        return toResponse(review);
    }

//    Delete Review
    @CacheEvict(value = "Review",key = "#contractId + ':id:' + #reviewerId",beforeInvocation = true)
    public void deleteReview(Long contractId,Long reviewerId){
//        find contract by ID
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("Contract not fond by ID : "+contractId));

//        Check Client and GIG authority
        boolean isClient = reviewerId.equals(contract.getClient().getId());
        boolean isGig = contract.getGig() != null && reviewerId.equals(contract.getGig().getUser().getId());

        if (!isClient && !isGig)
            throw new ForbiddenException("You aren't a participant of this contract ID : " + contractId);

//        Get Review by contract or reviewer ID
        Review review = reviewRepository.findByContractIdAndReviewerId(contractId, reviewerId).orElseThrow(() -> new ResourceNotFoundException("Review not found .."));

//        Check -> Contract must complete for deleting review
        if(!contract.getContractStatus().equals(ContractStatus.COMPLETE))
            throw new InvalidStatusException("Review can only be delete after contract is closed");

//        Adjust Rating
        adjustRating(review.getReviewee(), -review.getRating(), -1);
        reviewRepository.delete(review);
    }

//    Update Review
    @CachePut(value = "Review",key = "#contractId + ':id:' + #reviewerId")
    public ReviewResponseDTO updateReview(Long contractId, Long reviewerId, UserEntity reviewer, CreateReviewRequest dto){
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("Contract not found.."));

//        Check client and gig authority
        boolean isClient = reviewerId.equals(contract.getClient().getId());
        boolean isGig = contract.getGig() != null && reviewerId.equals(contract.getGig().getUser().getId());

        if (!isClient && !isGig)
            throw new ForbiddenException("You aren't a participant of this contract ID : " + contractId);

//        Contract must Complete for updating review
        if (!contract.getContractStatus().equals(ContractStatus.COMPLETE))
            throw new InvalidStatusException("Review can only be updated after contract is closed");

//        Find review by contract and reviewer id
        Review review = reviewRepository.findByContractIdAndReviewerId(contractId, reviewerId).orElseThrow(() -> new ResourceNotFoundException("Review not found .."));

//        comment mustn't be null or blank
        if (dto.getComment() != null && !dto.getComment().isBlank()) {
            review.setComment(dto.getComment());
        }

//        Rating mustn't be null
        if (dto.getRating() != null) {
            // CHANGED: 0 -> 1 to match the actual allowed range
            if (dto.getRating() < 1 || dto.getRating() > 5)
                throw new BadRequestException("Rating must be between 1 and 5");

//            set and adjust Rating
            Integer oldRating = review.getRating();
            if (!dto.getRating().equals(oldRating)) {
                review.setRating(dto.getRating());
                adjustRating(review.getReviewee(), dto.getRating() - oldRating, 0);
            }
        }
        return toResponse(review);
    }

//    Reviews
    public List<ReviewResponseDTO> reviews(int page,int size,String direction,String byFiled){
//        Get Current Logged-in Profile
        UserEntity user = authService.getCurrentProfile();

        String key = "MyReviews:id:"+user.getId()+":page:"+page+":direction:"+direction+":field:"+byFiled;

        try {
            String cache = stringRedisTemplate.opsForValue().get(key);
            if (cache != null && !cache.isBlank()) {
                return objectMapper.readValue(cache,new TypeReference<List<ReviewResponseDTO>>() {});
            }
        } catch (Exception e) {
            stringRedisTemplate.delete(key);
        }

//        Creating Pageable Object & Fetch current logged-in user Reviews
        Pageable pageable = PageRequest.of(page - 1, size, Sort.Direction.fromString(direction),byFiled);
        List<Review> reviewList = reviewRepository.findByRevieweeId(user.getId(),pageable).getContent();

//        Convert into a DTO list
        List<ReviewResponseDTO> reviewResponseDTOList = reviewList.stream().map(review -> ReviewResponseDTO.builder()
                .name(review.getReviewer().getFirstName() + " " + review.getReviewer().getLastName())
                .createdAt(review.getCreatedAt())
                .comment(review.getComment() == null ? "" : review.getComment())
                .rating(review.getRating())
                .id(review.getId())
                .build()
        ).toList();


        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(reviewResponseDTOList), 15,TimeUnit.MINUTES);
        } catch (Exception e) {
            stringRedisTemplate.delete(key);
        }
        return reviewResponseDTOList;
    }

    @Cacheable(value = "Review",key = "#contractId + ':id:' + #reviewerId")
    public ReviewResponseDTO getMyReview(Long contractId,Long reviewerId) {
        Review review = reviewRepository.findByContractIdAndReviewerId(contractId, reviewerId).orElseThrow(() -> new ResourceNotFoundException("Review not found by Contract ID : "+contractId));
        return toResponse(review);
    }

//    Adjust Rating
    private void adjustRating(UserEntity reviewee, long ratingDelta, long countDelta) {
//        Get total sum and count of Ratings
        long sum = reviewee.getTotalRatingSum() == null ? 0L : reviewee.getTotalRatingSum();
        long count = reviewee.getTotalRatings() == null ? 0L : reviewee.getTotalRatings();

//        Update Rating and count
        reviewee.setTotalRatingSum(sum + ratingDelta);
        reviewee.setTotalRatings(count + countDelta);

//        Save in DB
        userEntityRepository.save(reviewee);
    }


    private ReviewResponseDTO toResponse(Review review){
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .name(review.getReviewer().getFirstName() + " " + review.getReviewer().getLastName())
                .createdAt(review.getCreatedAt())
                .comment(review.getComment() == null ? "" : review.getComment())
                .rating(review.getRating())
                .build();
    }
}
