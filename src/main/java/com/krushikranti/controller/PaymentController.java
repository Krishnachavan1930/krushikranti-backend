package com.krushikranti.controller;

import com.krushikranti.dto.request.PaymentOrderRequest;
import com.krushikranti.dto.request.PaymentVerifyRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.PaymentOrderResponse;
import com.krushikranti.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Endpoints for handling Razorpay integrations")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    @Operation(summary = "Generate a Razorpay payment order for an existing internal order")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER')")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createOrder(
            @Valid @RequestBody PaymentOrderRequest request) {
        PaymentOrderResponse response = paymentService.createPaymentOrder(request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Razorpay payment order generated", response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify the authenticity of a Razorpay payment signature")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER')")
    public ResponseEntity<ApiResponse<Boolean>> verifyPayment(@Valid @RequestBody PaymentVerifyRequest request) {
        boolean verified = paymentService.verifyPaymentSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (verified) {
            return ResponseEntity.ok(ApiResponse.success("Payment successful and verified", true));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Payment verification failed"));
        }
    }
}
