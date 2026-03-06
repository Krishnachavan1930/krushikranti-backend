package com.krushikranti.controller;

import com.krushikranti.dto.request.OrderRequest;
import com.krushikranti.dto.request.OrderStatusUpdateRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.OrderResponse;
import com.krushikranti.service.OrderService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Endpoints for managing orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            Authentication authentication) {
        OrderResponse response = orderService.createOrder(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", response));
    }

    @GetMapping("/user")
    @Operation(summary = "Get all orders for the authenticated user")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(Authentication authentication) {
        List<OrderResponse> orders = orderService.getUserOrders(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("User orders fetched successfully", orders));
    }

    @GetMapping("/farmer")
    @Operation(summary = "Get all orders for a farmer's products")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getFarmerOrders(Authentication authentication) {
        List<OrderResponse> orders = orderService.getFarmerOrders(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Farmer orders fetched successfully", orders));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update an order's status")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        OrderResponse updated = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", updated));
    }
}
