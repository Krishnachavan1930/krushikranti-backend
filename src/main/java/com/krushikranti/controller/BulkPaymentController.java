package com.krushikranti.controller;

import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.BulkOrderResponse;
import com.krushikranti.dto.response.PaymentOrderResponse;
import com.krushikranti.service.BulkPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bulk-payments")
@RequiredArgsConstructor
@Tag(name = "Bulk Payments", description = "Payment endpoints for B2B bulk marketplace deals")
@SecurityRequirement(name = "bearerAuth")
public class BulkPaymentController {

    private final BulkPaymentService bulkPaymentService;

    @PostMapping("/initiate/{dealOfferId}")
    @Operation(summary = "Initiate payment for an accepted bulk deal")
    @PreAuthorize("hasRole('WHOLESALER')")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> initiatePayment(
            @PathVariable Long dealOfferId,
            Authentication authentication) {

        PaymentOrderResponse response = bulkPaymentService.initiatePayment(authentication.getName(), dealOfferId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment order created successfully", response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify payment and process order")
    @PreAuthorize("hasRole('WHOLESALER')")
    public ResponseEntity<ApiResponse<BulkOrderResponse>> verifyPayment(
            @RequestBody Map<String, String> payload) {

        String razorpayOrderId = payload.get("razorpay_order_id");
        String razorpayPaymentId = payload.get("razorpay_payment_id");
        String razorpaySignature = payload.get("razorpay_signature");

        BulkOrderResponse response = bulkPaymentService.verifyAndProcessPayment(
                razorpayOrderId, razorpayPaymentId, razorpaySignature);

        return ResponseEntity.ok(ApiResponse.success("Payment verified and order confirmed", response));
    }

    @GetMapping("/orders")
    @Operation(summary = "Get all bulk orders for the current user")
    @PreAuthorize("hasAnyRole('FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BulkOrderResponse>>> getOrders(
            Authentication authentication) {

        List<BulkOrderResponse> orders = bulkPaymentService.getOrders(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Bulk orders fetched successfully", orders));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get a specific bulk order")
    @PreAuthorize("hasAnyRole('FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BulkOrderResponse>> getOrder(
            @PathVariable Long orderId,
            Authentication authentication) {

        BulkOrderResponse order = bulkPaymentService.getOrder(authentication.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Bulk order fetched successfully", order));
    }
}
