package com.krushikranti.controller;

import com.krushikranti.dto.request.CheckoutPaymentInitiateRequest;
import com.krushikranti.dto.request.PaymentVerifyRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.PaymentOrderResponse;
import com.krushikranti.dto.response.PaymentVerificationResponse;
import com.krushikranti.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Endpoints for handling Razorpay integrations")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    @Operation(summary = "Generate a Razorpay payment order for checkout without persisting orders yet")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER')")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createOrder(
            @Valid @RequestBody CheckoutPaymentInitiateRequest request,
            Authentication authentication) {
        PaymentOrderResponse response = paymentService.createPaymentOrder(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Razorpay payment order generated", response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify the authenticity of a Razorpay payment signature")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER')")
    public ResponseEntity<ApiResponse<PaymentVerificationResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request,
            Authentication authentication) {
        PaymentVerificationResponse response = paymentService.verifyPaymentSignature(
                authentication.getName(),
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());
        return ResponseEntity.ok(ApiResponse.success("Payment successful and verified", response));
    }
}
