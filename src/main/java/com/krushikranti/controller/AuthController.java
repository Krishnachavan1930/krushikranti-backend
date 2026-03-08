package com.krushikranti.controller;

import com.krushikranti.dto.request.LoginRequest;
import com.krushikranti.dto.request.OtpVerifyRequest;
import com.krushikranti.dto.request.RegisterRequest;
import com.krushikranti.dto.request.ResendOtpRequest;
import com.krushikranti.dto.request.ForgotPasswordRequest;
import com.krushikranti.dto.request.VerifyResetOtpRequest;
import com.krushikranti.dto.request.ResetPasswordRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.AuthResponse;
import com.krushikranti.dto.response.VerifyResetOtpResponse;
import com.krushikranti.model.User;
import com.krushikranti.service.AuthService;
import com.krushikranti.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and email verification")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    @GetMapping("/test-email")
    @Operation(summary = "Test email configuration", description = "Sends a test email to verify SMTP configuration")
    public ResponseEntity<ApiResponse<String>> testEmail(@RequestParam String email) {
        log.info("Testing email to: {}", email);
        try {
            emailService.sendOtpEmail(email, "123456");
            return ResponseEntity.ok(ApiResponse.success("Test email sent to " + email, null));
        } catch (Exception e) {
            log.error("Email test failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Email sending failed: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with isVerified=false and sends OTP to email")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("Register request received: firstName={}, lastName={}, email={}, phone={}, role={}",
                request.getFirstName(), request.getLastName(), request.getEmail(), request.getPhone(),
                request.getRole());
        String message = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, null));
    }

    @PostMapping("/register-admin")
    @Operation(summary = "Register an Admin (Dev/Bootstrap)", description = "Directly creates an active admin user without OTP for development purposes")
    public ResponseEntity<ApiResponse<String>> registerAdmin(@Valid @RequestBody RegisterRequest request) {
        log.warn("Admin registration attempt: email={}", request.getEmail());
        // Force the role to ADMIN
        request.setRole(User.Role.ROLE_ADMIN);
        String message = authService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, null));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify email with OTP", description = "Verifies user email using the 6-digit OTP sent during registration")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        String message = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP", description = "Generates and sends a new OTP to the user's email")
    public ResponseEntity<ApiResponse<String>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        String message = authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates a verified user and returns JWT tokens. Email must be verified first.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password", description = "Sends a password reset OTP to the user's email")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password endpoint called for email: {}", request.getEmail());
        try {
            String message = authService.forgotPassword(request);
            log.info("Forgot password successful for: {}", request.getEmail());
            return ResponseEntity.ok(ApiResponse.success(message, null));
        } catch (Exception e) {
            log.error("Forgot password failed for {}: {}", request.getEmail(), e.getMessage(), e);
            throw e; // Re-throw to let GlobalExceptionHandler handle it
        }
    }

    @PostMapping("/verify-reset-otp")
    @Operation(summary = "Verify Reset OTP", description = "Verifies the password reset OTP and returns a reset token")
    public ResponseEntity<ApiResponse<VerifyResetOtpResponse>> verifyResetOtp(
            @Valid @RequestBody VerifyResetOtpRequest request) {
        VerifyResetOtpResponse response = authService.verifyResetOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset Password", description = "Resets the user's password using the reset token")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String message = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }
}
