package com.krushikranti.controller;

import com.krushikranti.dto.request.OrderRequest;
import com.krushikranti.dto.request.OrderStatusUpdateRequest;
import com.krushikranti.dto.request.AssignDeliveryPartnerRequest;
import com.krushikranti.dto.request.DeliveryStatusUpdateRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.OrderResponse;
import com.krushikranti.dto.response.TrackingResponse;
import com.krushikranti.model.Order;
import com.krushikranti.service.OrderService;
import com.krushikranti.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
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
    private final InvoiceService invoiceService;

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

    @GetMapping("/user/paginated")
    @Operation(summary = "Get paginated orders for the authenticated user")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrdersPaginated(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Order.OrderStatus status) {
        Page<OrderResponse> orders = orderService.getUserOrdersPaginated(authentication.getName(), page, size, status);
        return ResponseEntity.ok(ApiResponse.success("User orders fetched successfully", orders));
    }

    @GetMapping("/farmer")
    @Operation(summary = "Get all orders for a farmer's products")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getFarmerOrders(Authentication authentication) {
        List<OrderResponse> orders = orderService.getFarmerOrders(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Farmer orders fetched successfully", orders));
    }

    @GetMapping("/farmer/paginated")
    @Operation(summary = "Get paginated orders for a farmer's products")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getFarmerOrdersPaginated(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Order.OrderStatus status) {
        Page<OrderResponse> orders = orderService.getFarmerOrdersPaginated(authentication.getName(), page, size,
                status);
        return ResponseEntity.ok(ApiResponse.success("Farmer orders fetched successfully", orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("Order fetched successfully", order));
    }

    @GetMapping("/{orderId}/track")
    @Operation(summary = "Get tracking information for an order by ID")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TrackingResponse>> getOrderTracking(@PathVariable Long orderId) {
        TrackingResponse tracking = orderService.getOrderTracking(orderId);
        return ResponseEntity.ok(ApiResponse.success("Tracking information fetched successfully", tracking));
    }

    @GetMapping("/{orderId}/invoice")
    @Operation(summary = "Download invoice PDF for an order (legacy path)")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<Resource> downloadInvoice(
            @PathVariable Long orderId,
            Authentication authentication) {
        Resource resource = invoiceService.getInvoiceResourceForUser(orderId, authentication.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + orderId + ".pdf")
                .body(resource);
    }

    @GetMapping("/track/{orderNumber}")
    @Operation(summary = "Get tracking information for an order by order number")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TrackingResponse>> getOrderTrackingByNumber(@PathVariable String orderNumber) {
        TrackingResponse tracking = orderService.getOrderTrackingByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Tracking information fetched successfully", tracking));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order (only if pending)")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            Authentication authentication) {
        OrderResponse cancelled = orderService.cancelOrder(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", cancelled));
    }

    @GetMapping
    @Operation(summary = "Get paginated orders for authenticated user")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Order.OrderStatus status) {
        Page<OrderResponse> orders = orderService.getUserOrdersPaginated(authentication.getName(), page, size, status);
        return ResponseEntity.ok(ApiResponse.success("Orders fetched successfully", orders));
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

    @PostMapping("/{id}/create-shipment")
    @Operation(summary = "Create shipment for a paid order")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> createShipment(@PathVariable Long id) {
        OrderResponse updated = orderService.processPaymentAndCreateShipment(id);
        return ResponseEntity.ok(ApiResponse.success("Shipment created successfully", updated));
    }

    // ── Delivery Partner Endpoints ───────────────────────────────────────────

    @PostMapping("/{id}/assign-delivery")
    @Operation(summary = "Assign a delivery partner to an order (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> assignDeliveryPartner(
            @PathVariable Long id,
            @Valid @RequestBody AssignDeliveryPartnerRequest request) {
        OrderResponse updated = orderService.assignDeliveryPartner(id, request.getDeliveryPartnerId(),
                request.getNotes());
        return ResponseEntity.ok(ApiResponse.success("Delivery partner assigned successfully", updated));
    }

    @PutMapping("/{id}/delivery-status")
    @Operation(summary = "Update delivery status (Delivery partner or Admin)")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateDeliveryStatus(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryStatusUpdateRequest request) {
        OrderResponse updated = orderService.updateDeliveryStatus(id, request.getDeliveryStatus());
        return ResponseEntity.ok(ApiResponse.success("Delivery status updated successfully", updated));
    }

    @GetMapping("/delivery/assigned")
    @Operation(summary = "Get orders assigned to authenticated delivery partner")
    @PreAuthorize("hasAnyRole('USER', 'FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getDeliveryPartnerOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Order.DeliveryStatus deliveryStatus) {
        Page<OrderResponse> orders = orderService.getDeliveryPartnerOrders(authentication.getName(), page, size,
                deliveryStatus);
        return ResponseEntity.ok(ApiResponse.success("Delivery orders fetched successfully", orders));
    }

    // ── Admin Endpoints ──────────────────────────────────────────────────────

    @GetMapping("/admin/all")
    @Operation(summary = "Get all orders (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Order.OrderStatus status) {
        Page<OrderResponse> orders = orderService.getAllOrders(page, size, status);
        return ResponseEntity.ok(ApiResponse.success("All orders fetched successfully", orders));
    }

    @GetMapping("/admin/stats")
    @Operation(summary = "Get admin dashboard statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderService.AdminOrderStats>> getAdminStats() {
        OrderService.AdminOrderStats stats = orderService.getAdminStats();
        return ResponseEntity.ok(ApiResponse.success("Admin stats fetched successfully", stats));
    }

    @GetMapping("/farmer/stats")
    @Operation(summary = "Get farmer dashboard statistics")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderService.FarmerOrderStats>> getFarmerStats(Authentication authentication) {
        OrderService.FarmerOrderStats stats = orderService.getFarmerStats(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Farmer stats fetched successfully", stats));
    }
}
