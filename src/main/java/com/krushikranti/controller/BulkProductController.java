package com.krushikranti.controller;

import com.krushikranti.dto.request.CreateBulkProductRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.BulkProductResponse;
import com.krushikranti.service.BulkProductService;
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
@RequestMapping("/api/v1/bulk-products")
@RequiredArgsConstructor
@Tag(name = "Bulk Products", description = "Endpoints for bulk marketplace product listings")
@SecurityRequirement(name = "bearerAuth")
public class BulkProductController {

    private final BulkProductService bulkProductService;

    @PostMapping
    @Operation(summary = "Create a bulk product listing")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<BulkProductResponse>> createBulkProduct(
            @Valid @RequestBody CreateBulkProductRequest request,
            Authentication authentication) {

        BulkProductResponse response = bulkProductService.createBulkProduct(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bulk product created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all active bulk products")
    public ResponseEntity<ApiResponse<List<BulkProductResponse>>> getAllActiveBulkProducts() {
        List<BulkProductResponse> products = bulkProductService.getAllActiveBulkProducts();
        return ResponseEntity.ok(ApiResponse.success("Bulk products fetched successfully", products));
    }

    @GetMapping("/my-products")
    @Operation(summary = "Get farmer's own bulk products")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<List<BulkProductResponse>>> getMyBulkProducts(
            Authentication authentication) {

        List<BulkProductResponse> products = bulkProductService.getMyBulkProducts(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("My bulk products fetched successfully", products));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a bulk product")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<BulkProductResponse>> updateBulkProduct(
            @PathVariable Long id,
            @Valid @RequestBody CreateBulkProductRequest request,
            Authentication authentication) {

        BulkProductResponse response = bulkProductService.updateBulkProduct(authentication.getName(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Bulk product updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a bulk product")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<Void>> deleteBulkProduct(
            @PathVariable Long id,
            Authentication authentication) {

        bulkProductService.deleteBulkProduct(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Bulk product deleted successfully", null));
    }
}
