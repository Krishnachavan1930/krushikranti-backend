package com.krushikranti.controller;

import com.krushikranti.dto.request.FarmerRatingRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.FarmerRatingResponse;
import com.krushikranti.service.FarmerRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/farmer-ratings")
@RequiredArgsConstructor
public class FarmerRatingController {

    private final FarmerRatingService farmerRatingService;

    /**
     * Wholesaler submits a rating for a farmer after order is DELIVERED.
     */
    @PostMapping
    @PreAuthorize("hasRole('WHOLESALER')")
    public ResponseEntity<ApiResponse<FarmerRatingResponse>> submitRating(
            @Valid @RequestBody FarmerRatingRequest request,
            Principal principal) {
        FarmerRatingResponse response = farmerRatingService.submitRating(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rating submitted successfully", response));
    }

    /**
     * Get all ratings for a specific farmer.
     */
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<ApiResponse<List<FarmerRatingResponse>>> getFarmerRatings(
            @PathVariable Long farmerId) {
        List<FarmerRatingResponse> ratings = farmerRatingService.getFarmerRatings(farmerId);
        return ResponseEntity.ok(ApiResponse.success("Ratings fetched successfully", ratings));
    }

    /**
     * Get average rating and total count for a farmer.
     */
    @GetMapping("/farmer/{farmerId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFarmerRatingStats(
            @PathVariable Long farmerId) {
        Map<String, Object> stats = farmerRatingService.getFarmerRatingStats(farmerId);
        return ResponseEntity.ok(ApiResponse.success("Rating stats fetched", stats));
    }
}
