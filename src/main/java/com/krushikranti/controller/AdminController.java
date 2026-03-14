package com.krushikranti.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.krushikranti.dto.request.AdminChangePasswordRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.model.User;
import com.krushikranti.repository.OrderRepository;
import com.krushikranti.repository.ProductRepository;
import com.krushikranti.repository.UserRepository;
import com.krushikranti.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({ "/api/v1/admin", "/api/admin" })
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Endpoints for Admin Dashboard APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AuthService authService;

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get admin dashboard statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        List<User> allUsers = userRepository.findAll();

        long totalUsers = allUsers.size();
        long totalFarmers = allUsers.stream().filter(u -> u.getRole() == User.Role.ROLE_FARMER).count();
        long totalWholesalers = allUsers.stream().filter(u -> u.getRole() == User.Role.ROLE_WHOLESALER).count();
        long totalOrders = orderRepository.count();
        long totalProducts = productRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalFarmers", totalFarmers);
        stats.put("totalWholesalers", totalWholesalers);
        stats.put("totalOrders", totalOrders);
        stats.put("totalProducts", totalProducts);

        return ResponseEntity.ok(ApiResponse.success("Dashboard stats fetched successfully", stats));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change admin password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody AdminChangePasswordRequest request,
            Authentication authentication) {
        String message = authService.changeAdminPassword(
                authentication.getName(),
                request.getCurrentPassword(),
                request.getNewPassword());

        return ResponseEntity.ok(ApiResponse.success(message, null));
    }
}
