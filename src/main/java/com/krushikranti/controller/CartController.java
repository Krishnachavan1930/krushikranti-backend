package com.krushikranti.controller;

import com.krushikranti.dto.request.AddToCartRequest;
import com.krushikranti.dto.request.UpdateCartRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.CartItemResponse;
import com.krushikranti.dto.response.CartResponse;
import com.krushikranti.service.CartService;
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
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER', 'WHOLESALER')")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get cart items", description = "Returns all items in the authenticated user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Authentication authentication) {
        String email = authentication.getName();
        CartResponse cart = cartService.getCart(email);
        return ResponseEntity.ok(ApiResponse.success("Cart fetched successfully", cart));
    }

    @PostMapping("/add")
    @Operation(summary = "Add item to cart", description = "Adds a product to cart. If already exists, increases quantity.")
    public ResponseEntity<ApiResponse<CartItemResponse>> addToCart(
            Authentication authentication,
            @Valid @RequestBody AddToCartRequest request) {
        String email = authentication.getName();
        CartItemResponse item = cartService.addToCart(email, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product added to cart", item));
    }

    @PutMapping("/update")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateCartItem(
            Authentication authentication,
            @Valid @RequestBody UpdateCartRequest request) {
        String email = authentication.getName();
        CartItemResponse item = cartService.updateCartItem(email, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", item));
    }

    @DeleteMapping("/remove/{productId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            Authentication authentication,
            @PathVariable Long productId) {
        String email = authentication.getName();
        cartService.removeCartItem(email, productId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", null));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear entire cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication authentication) {
        String email = authentication.getName();
        cartService.clearCart(email);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }

    @GetMapping("/count")
    @Operation(summary = "Get cart item count (for badge)")
    public ResponseEntity<ApiResponse<Long>> getCartItemCount(Authentication authentication) {
        String email = authentication.getName();
        long count = cartService.getCartItemCount(email);
        return ResponseEntity.ok(ApiResponse.success("Cart count fetched", count));
    }
}
