package com.krushikranti.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:KrushiKranti}")
    private String appName;

    /**
     * Send OTP verification email
     * 
     * @param toEmail recipient email address
     * @param otp     the 6-digit OTP code
     */
    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(appName + " Email Verification");
            message.setText(buildOtpEmailBody(otp));

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }

    /**
     * Send welcome email after successful verification
     * 
     * @param toEmail  recipient email address
     * @param userName user's name
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to " + appName + "!");
            message.setText(buildWelcomeEmailBody(userName));

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
        }
    }

    /**
     * Send password reset OTP email
     * 
     * @param toEmail recipient email address
     * @param otp     the 6-digit OTP code
     */
    @Async
    public void sendPasswordResetOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(appName + " Password Reset");
            message.setText(buildPasswordResetEmailBody(otp));

            mailSender.send(message);
            log.info("Password reset OTP email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to: {}", toEmail, e);
            // Don't throw - let the async method fail silently rather than crash
        }
    }

    /**
     * Send password reset OTP email (synchronous version for forgot-password flow)
     * 
     * @param toEmail recipient email address
     * @param otp     the 6-digit OTP code
     */
    public void sendPasswordResetOtp(String toEmail, String otp) {
        try {
            log.info("Sending password reset OTP to: {}", toEmail);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(appName + " Password Reset OTP");
            message.setText(buildPasswordResetEmailBody(otp));

            mailSender.send(message);
            log.info("Password reset OTP sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to: {} - Error: {}", toEmail, e.getMessage(), e);
            // Log but don't throw - OTP is already saved in database
        }
    }

    /**
     * Send password reset confirmation email
     * 
     * @param toEmail  recipient email address
     * @param userName user's name
     */
    @Async
    public void sendPasswordResetConfirmationEmail(String toEmail, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(appName + " Password Reset Successful");
            message.setText(buildPasswordResetConfirmationEmailBody(userName));

            mailSender.send(message);
            log.info("Password reset confirmation email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset confirmation email to: {}", toEmail, e);
        }
    }

    private String buildOtpEmailBody(String otp) {
        return String.format("""
                Dear User,

                Your OTP for email verification is: %s

                This OTP will expire in 5 minutes.

                If you did not request this verification, please ignore this email.

                Best regards,
                %s Team
                """, otp, appName);
    }

    private String buildWelcomeEmailBody(String userName) {
        return String.format("""
                Dear %s,

                Welcome to %s!

                Your email has been successfully verified. You can now access all features of our platform.

                Thank you for joining our agricultural marketplace community.

                Best regards,
                %s Team
                """, userName, appName, appName);
    }

    private String buildPasswordResetEmailBody(String otp) {
        return String.format("""
                Dear User,

                Your OTP for password reset is: %s

                This OTP will expire in 5 minutes.

                If you did not request a password reset, please ignore this email and ensure your account is secure.

                Best regards,
                %s Team
                """, otp, appName);
    }

    private String buildPasswordResetConfirmationEmailBody(String userName) {
        return String.format("""
                Dear %s,

                Your password has been successfully reset.

                You can now login to your account using your new password.

                If you did not perform this action, please contact support immediately.

                Best regards,
                %s Team
                """, userName, appName);
    }
}
