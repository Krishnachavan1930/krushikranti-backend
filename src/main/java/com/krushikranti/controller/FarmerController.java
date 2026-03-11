package com.krushikranti.controller;

import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.service.FarmerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/farmer")
@RequiredArgsConstructor
@Tag(name = "Farmer", description = "Farmer dashboard and analytics endpoints")
@SecurityRequirement(name = "bearerAuth")
public class FarmerController {

    private final FarmerService farmerService;

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get farmer dashboard analytics: revenue, orders, completed, active, top product")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats(
            Authentication authentication) {
        Map<String, Object> stats = farmerService.getDashboardStats(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Farmer dashboard stats fetched successfully", stats));
    }
}
