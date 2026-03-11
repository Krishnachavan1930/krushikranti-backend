package com.krushikranti.controller;

import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.model.AdminLog;
import com.krushikranti.service.AdminLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
@Tag(name = "Admin Logs", description = "Admin activity audit log endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminLogController {

    private final AdminLogService adminLogService;

    @GetMapping
    @Operation(summary = "Get paginated admin activity logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AdminLog>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type) {
        Page<AdminLog> logs = adminLogService.getLogs(page, size, search, type);
        return ResponseEntity.ok(ApiResponse.success("Admin logs fetched successfully", logs));
    }
}
