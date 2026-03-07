package com.krushikranti.controller;

import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.UserResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.User;
import com.krushikranti.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "User profile management")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", UserResponse.fromEntity(user)));
    }

    @GetMapping
    @Operation(summary = "Get all users (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Page<User> users = userRepository.findAll(PageRequest.of(page, size, sort));
        Page<UserResponse> userResponses = users.map(UserResponse::fromEntity);
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", userResponses));
    }

    @PutMapping("/{id}/ban")
    @Operation(summary = "Ban a user (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> banUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        // Prevent banning admin users
        if (user.getRole() == User.Role.ROLE_ADMIN) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cannot ban admin users"));
        }
        
        user.setEnabled(false);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("User banned successfully", UserResponse.fromEntity(user)));
    }

    @PutMapping("/{id}/unban")
    @Operation(summary = "Unban a user (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> unbanUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setEnabled(true);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("User unbanned successfully", UserResponse.fromEntity(user)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        // Prevent deleting admin users
        if (user.getRole() == User.Role.ROLE_ADMIN) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cannot delete admin users"));
        }
        
        userRepository.delete(user);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
}
