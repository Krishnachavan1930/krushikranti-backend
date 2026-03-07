package com.krushikranti.controller;

import com.krushikranti.dto.request.CreateProductRequest;
import com.krushikranti.dto.request.UpdateProductRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.ProductResponse;
import com.krushikranti.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Endpoints for managing agricultural products")
public class ProductController {

    private final ProductService productService;

    // ────────────────────────────────────────────────────────────────
    // PUBLIC ENDPOINTS
    // ────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get all products (public)", description = "Returns paginated list of active products with optional filters")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> products = productService.getAllProducts(category, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", products));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID (public)")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product fetched successfully", product));
    }

    // ────────────────────────────────────────────────────────────────
    // FARMER ENDPOINTS
    // ────────────────────────────────────────────────────────────────

    @GetMapping("/my-products")
    @Operation(summary = "Get my products (Farmer only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getMyProducts(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String email = authentication.getName();
        Page<ProductResponse> products = productService.getMyProducts(email, pageable);
        return ResponseEntity.ok(ApiResponse.success("Farmer products fetched successfully", products));
    }

    @GetMapping("/farmer/{farmerId}")
    @Operation(summary = "Get products by farmer ID")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProductsByFarmer(
            @PathVariable Long farmerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProductResponse> products = productService.getProductsByFarmer(farmerId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Farmer products fetched successfully", products));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a product (Farmer only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            Authentication authentication,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("retailPrice") java.math.BigDecimal retailPrice,
            @RequestParam(value = "wholesalePrice", required = false) java.math.BigDecimal wholesalePrice,
            @RequestParam("quantity") Integer quantity,
            @RequestParam("unit") String unit,
            @RequestParam("category") String category,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "organic", required = false, defaultValue = "false") Boolean organic,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {

        CreateProductRequest request = CreateProductRequest.builder()
                .name(name)
                .description(description)
                .retailPrice(retailPrice)
                .wholesalePrice(wholesalePrice)
                .quantity(quantity)
                .unit(unit)
                .category(category)
                .imageUrl(imageUrl)
                .location(location)
                .organic(organic)
                .build();

        String email = authentication.getName();
        ProductResponse created = productService.createProduct(request, email, imageFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", created));
    }

    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a product with JSON body (Farmer only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProductJson(
            Authentication authentication,
            @Valid @RequestBody CreateProductRequest request) {

        String email = authentication.getName();
        ProductResponse created = productService.createProduct(request, email, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update a product (multipart)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "retailPrice", required = false) java.math.BigDecimal retailPrice,
            @RequestParam(value = "wholesalePrice", required = false) java.math.BigDecimal wholesalePrice,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "organic", required = false) Boolean organic,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name(name)
                .description(description)
                .retailPrice(retailPrice)
                .wholesalePrice(wholesalePrice)
                .quantity(quantity)
                .unit(unit)
                .category(category)
                .imageUrl(imageUrl)
                .location(location)
                .organic(organic)
                .status(status)
                .build();

        String email = authentication.getName();
        ProductResponse updated = productService.updateProduct(id, request, email, imageFile);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updated));
    }

    @PutMapping(value = "/{id}/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a product with JSON body")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductJson(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody UpdateProductRequest request) {

        String email = authentication.getName();
        ProductResponse updated = productService.updateProduct(id, request, email, null);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        productService.deleteProduct(id, email);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get available product categories")
    public ResponseEntity<ApiResponse<String[]>> getCategories() {
        String[] categories = { "vegetables", "fruits", "grains", "pulses", "spices", "dairy", "other" };
        return ResponseEntity.ok(ApiResponse.success("Categories fetched", categories));
    }
}
