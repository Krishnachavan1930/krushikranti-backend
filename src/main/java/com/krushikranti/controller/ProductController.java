package com.krushikranti.controller;

import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.model.Product;
import com.krushikranti.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Endpoints for managing agricultural products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all products", description = "Returns paginated list of active products")
    public ResponseEntity<ApiResponse<Page<Product>>> getAllProducts(
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
        Page<Product> products = productService.getAllProducts(category, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", products));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product fetched successfully", product));
    }

    @GetMapping("/farmer/{farmerId}")
    @Operation(summary = "Get products by farmer")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<Product>>> getProductsByFarmer(
            @PathVariable Long farmerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productService.getProductsByFarmer(farmerId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Farmer products fetched successfully", products));
    }

    @PostMapping
    @Operation(summary = "Create a product (Farmer only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @RequestBody Product product,
            @RequestParam Long farmerId) {
        Product created = productService.createProduct(product, farmerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable Long id,
            @RequestBody Product product) {
        Product updated = productService.updateProduct(id, product);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }
}
