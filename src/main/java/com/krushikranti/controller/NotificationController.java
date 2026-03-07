package com.krushikranti.controller;

import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.NotificationResponse;
import com.krushikranti.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints for managing notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications for the authenticated user")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(Authentication authentication) {
        List<NotificationResponse> notifications = notificationService.getUserNotifications(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully", notifications));
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get paginated notifications for the authenticated user")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotificationsPaginated(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<NotificationResponse> notifications = notificationService.getUserNotificationsPaginated(
                authentication.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched successfully", notifications));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication authentication) {
        long count = notificationService.getUnreadCount(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched", Map.of("count", count)));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        notificationService.markAsRead(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PutMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}
