package com.krushikranti.service;

import com.krushikranti.dto.request.LoginRequest;
import com.krushikranti.dto.request.OtpVerifyRequest;
import com.krushikranti.dto.request.RegisterRequest;
import com.krushikranti.dto.request.ResendOtpRequest;
import com.krushikranti.dto.request.ForgotPasswordRequest;
import com.krushikranti.dto.request.VerifyResetOtpRequest;
import com.krushikranti.dto.request.ResetPasswordRequest;
import com.krushikranti.dto.response.AuthResponse;
import com.krushikranti.dto.response.VerifyResetOtpResponse;
import com.krushikranti.exception.DuplicateResourceException;
import com.krushikranti.exception.EmailNotVerifiedException;
import com.krushikranti.exception.InvalidOtpException;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.User;
import com.krushikranti.repository.UserRepository;
import com.krushikranti.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered: " + request.getEmail());
        }

        // Generate OTP
        String otp = generateOtp();
        LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        // Build user with isVerified = false
        User user = User.builder()
                .name(request.getFirstName() + " " + request.getLastName())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .isVerified(false)
                .otp(otp)
                .otpExpiryTime(otpExpiry)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered (pending verification): {} [{}]", savedUser.getEmail(), savedUser.getRole());

        // Send OTP email
        emailService.sendOtpEmail(savedUser.getEmail(), otp);

        return "Registration successful. Please verify your email with the OTP sent to " + request.getEmail();
    }

    @Transactional
    public String verifyOtp(OtpVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Check if already verified
        if (user.isVerified()) {
            return "Email is already verified. You can login now.";
        }

        // Validate OTP
        if (user.getOtp() == null || !user.getOtp().equals(request.getOtp())) {
            throw new InvalidOtpException("Invalid OTP. Please check and try again.");
        }

        // Check OTP expiry
        if (user.getOtpExpiryTime() == null || LocalDateTime.now().isAfter(user.getOtpExpiryTime())) {
            throw new InvalidOtpException("OTP has expired. Please request a new OTP.");
        }

        // Mark as verified and clear OTP
        user.setVerified(true);
        user.setOtp(null);
        user.setOtpExpiryTime(null);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", user.getEmail());

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());

        return "Email verified successfully. You can now login.";
    }

    @Transactional
    public String resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Check if already verified
        if (user.isVerified()) {
            return "Email is already verified. You can login now.";
        }

        // Generate new OTP
        String otp = generateOtp();
        LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        user.setOtp(otp);
        user.setOtpExpiryTime(otpExpiry);
        userRepository.save(user);

        // Send new OTP email
        emailService.sendOtpEmail(user.getEmail(), otp);

        log.info("OTP resent for user: {}", user.getEmail());

        return "OTP has been resent to " + request.getEmail();
    }

    public AuthResponse login(LoginRequest request) {
        // First check if user exists and is verified
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Check email verification status
        if (!user.isVerified()) {
            throw new EmailNotVerifiedException("Please verify your email before logging in.");
        }

        // Authenticate
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserDetails userDetails = toUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request received for email: {}", request.getEmail());
        
        // Validate email
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        log.info("User found for password reset: {}", user.getEmail());

        // Generate OTP for password reset
        String otp = generateOtp();
        log.info("Generated OTP for user {}", user.getEmail());
        
        LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        user.setOtp(otp);
        user.setOtpExpiryTime(otpExpiry);
        userRepository.save(user);
        log.info("OTP saved for user: {}", user.getEmail());

        // Send password reset OTP email (non-blocking)
        try {
            emailService.sendPasswordResetOtp(user.getEmail(), otp);
            log.info("Password reset OTP email triggered for: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to {}: {}", user.getEmail(), e.getMessage());
            // Don't throw - OTP is saved, user can request resend if email fails
        }

        return "Password reset OTP sent to your email.";
    }

    @Transactional
    public VerifyResetOtpResponse verifyResetOtp(VerifyResetOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Validate OTP
        if (user.getOtp() == null || !user.getOtp().equals(request.getOtp())) {
            throw new InvalidOtpException("Invalid OTP. Please check and try again.");
        }

        // Check OTP expiry
        if (user.getOtpExpiryTime() == null || LocalDateTime.now().isAfter(user.getOtpExpiryTime())) {
            throw new InvalidOtpException("OTP has expired. Please request a new OTP.");
        }

        // Generate reset token (valid for 15 minutes)
        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiryTime(LocalDateTime.now().plusMinutes(15));
        user.setOtp(null);
        user.setOtpExpiryTime(null);
        userRepository.save(user);

        log.info("Reset OTP verified for user: {}", user.getEmail());

        return VerifyResetOtpResponse.builder()
                .resetToken(resetToken)
                .message("OTP verified successfully. You can now reset your password.")
                .build();
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Validate reset token
        if (user.getResetToken() == null || !user.getResetToken().equals(request.getResetToken())) {
            throw new InvalidOtpException("Invalid or expired reset token. Please request a new password reset.");
        }

        // Check reset token expiry
        if (user.getResetTokenExpiryTime() == null || LocalDateTime.now().isAfter(user.getResetTokenExpiryTime())) {
            throw new InvalidOtpException("Reset token has expired. Please request a new password reset.");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiryTime(null);
        userRepository.save(user);

        log.info("Password reset successful for user: {}", user.getEmail());

        // Send confirmation email
        emailService.sendPasswordResetConfirmationEmail(user.getEmail(), user.getFirstName());

        return "Password has been reset successfully. You can now login with your new password.";
    }

    /**
     * Generate a secure 6-digit OTP
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name().replace("ROLE_", ""))
                .accountExpired(!user.isAccountNonExpired())
                .accountLocked(!user.isAccountNonLocked())
                .credentialsExpired(!user.isCredentialsNonExpired())
                .disabled(!user.isEnabled())
                .build();
    }
}
