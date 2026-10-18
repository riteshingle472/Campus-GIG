package org.riteshingle.campusgig.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.Model.UserEntity;
import org.riteshingle.campusgig.RequestDTO.CreateReviewRequest;
import org.riteshingle.campusgig.ResponseDTO.ReviewResponseDTO;
import org.riteshingle.campusgig.Service.AuthService;
import org.riteshingle.campusgig.Service.ReviewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final AuthService authService;

//    Review
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @PostMapping("/review/{contractId}")
    public ResponseEntity<ReviewResponseDTO> createReview(@PathVariable Long contractId,@Valid @RequestBody CreateReviewRequest request) {
        UserEntity reviewer = authService.getCurrentProfile();
        return ResponseEntity.ok(reviewService.postReview(contractId,reviewer.getId(),reviewer, request));
    }

//    Delete Review
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @DeleteMapping("/review/{contractId}")
    public ResponseEntity<?> deleteReview(@PathVariable Long contractId){
        Long reviewerId = authService.getCurrentProfile().getId();
        reviewService.deleteReview(contractId,reviewerId);
        return ResponseEntity.noContent().build();
    }

//    Update Review
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @PatchMapping("/review/{contractId}")
    public ResponseEntity<?> updateReview(@PathVariable Long contractId,@Valid @RequestBody CreateReviewRequest request){
        UserEntity reviewer = authService.getCurrentProfile();
        ReviewResponseDTO reviewResponseDTO = reviewService.updateReview(contractId,reviewer.getId(),reviewer, request);
        return ResponseEntity.noContent().build();
    }

//    Get my Reviews
    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @GetMapping("/review/{contractId}/mine")
    public ResponseEntity<ReviewResponseDTO> getMyReview(@PathVariable Long contractId) {
        Long reviewerId = authService.getCurrentProfile().getId();
        return ResponseEntity.ok(reviewService.getMyReview(contractId,reviewerId));
    }

    @PreAuthorize("hasRole('CLIENT') or hasRole('GIG')")
    @GetMapping("/reviews")
    public ResponseEntity<List<ReviewResponseDTO>> reviews(@RequestParam(required = false,defaultValue = "1") int page ,
                                                           @RequestParam(required = false,defaultValue = "10") int size,
                                                           @RequestParam(required = false,defaultValue = "DESC") String direction,
                                                           @RequestParam(required = false,defaultValue = "createdAt") String byFiled){


        return ResponseEntity.ok(reviewService.reviews(page,size,direction,byFiled));
    }
}