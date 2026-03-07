package com.krushikranti.controller;

import com.krushikranti.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Endpoints for Admin Dashboard APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get admin dashboard statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        // Return dummy statistics to satisfy the frontend admin dashboard API calls
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 1450);
        stats.put("totalFarmers", 850);
        stats.put("totalWholesalers", 400);
        stats.put("totalOrders", 3200);
        stats.put("totalRevenue", 2500000.00);
        stats.put("activeNegotiations", 125);

        return ResponseEntity.ok(ApiResponse.success("Dashboard stats fetched successfully", stats));
    }
}
