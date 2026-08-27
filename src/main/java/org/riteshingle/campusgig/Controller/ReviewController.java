package org.riteshingle.campusgig.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.riteshingle.campusgig.RequestDTO.CreateReviewRequest;
import org.riteshingle.campusgig.ResponseDTO.ReviewResponseDTO;
import org.riteshingle.campusgig.Service.ReviewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/review/{contractId}")
    public ResponseEntity<?> createReview(@PathVariable Long contractId,@Valid @RequestBody CreateReviewRequest request) {
        reviewService.postReview(contractId,request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/review/{contractId}")
    public ResponseEntity<?> deleteReview(@PathVariable Long contractId){
        reviewService.deleteReview(contractId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/review/{contractId}")
    public ResponseEntity<?> updateReview(@PathVariable Long contractId,@Valid @RequestBody CreateReviewRequest request){
        reviewService.updateReview(contractId,request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<ReviewResponseDTO>> reviews(@RequestParam(required = false,defaultValue = "1") int pageNumber ,
                                                           @RequestParam(required = false,defaultValue = "10") int size,
                                                           @RequestParam(required = false,defaultValue = "DESC") String direction,
                                                           @RequestParam(required = false,defaultValue = "createdAt") String byFiled){


        Pageable pageable = PageRequest.of(pageNumber - 1, size, Sort.Direction.fromString(direction),byFiled);
        return ResponseEntity.ok(reviewService.reviews(pageable));
    }
}