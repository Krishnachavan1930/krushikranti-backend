package com.krushikranti.controller;

import com.krushikranti.dto.request.CreateReviewRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.ReviewResponse;
import com.krushikranti.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product review management endpoints")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Create a product review", description = "Only authenticated users (ROLE_USER) can create reviews")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            Authentication authentication,
            @Valid @RequestBody CreateReviewRequest request) {
        String email = authentication.getName();
        ReviewResponse review = reviewService.createReview(request, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review created successfully", review));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get reviews for a product", description = "Returns all reviews for a specific product (public)")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("Reviews fetched successfully", reviews));
    }

    @GetMapping("/product/{productId}/stats")
    @Operation(summary = "Get review statistics for a product", description = "Returns average rating and review count")
    public ResponseEntity<ApiResponse<ReviewStatsResponse>> getProductReviewStats(
            @PathVariable Long productId) {
        Double averageRating = reviewService.getAverageRating(productId);
        Long totalReviews = reviewService.getReviewCount(productId);
        
        ReviewStatsResponse stats = new ReviewStatsResponse(averageRating, totalReviews);
        return ResponseEntity.ok(ApiResponse.success("Review stats fetched successfully", stats));
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete a review", description = "Only the review author or admin can delete a review")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            Authentication authentication,
            @PathVariable Long reviewId) {
        String email = authentication.getName();
        reviewService.deleteReview(reviewId, email);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }

    // Inner class for stats response
    public record ReviewStatsResponse(Double averageRating, Long totalReviews) {}
}
