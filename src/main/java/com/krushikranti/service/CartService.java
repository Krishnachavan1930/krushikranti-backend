package com.krushikranti.service;

import com.krushikranti.dto.request.AddToCartRequest;
import com.krushikranti.dto.request.UpdateCartRequest;
import com.krushikranti.dto.response.CartItemResponse;
import com.krushikranti.dto.response.CartResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.CartItem;
import com.krushikranti.model.Product;
import com.krushikranti.model.User;
import com.krushikranti.repository.CartRepository;
import com.krushikranti.repository.ProductRepository;
import com.krushikranti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Get the current user's cart
     */
    public CartResponse getCart(String userEmail) {
        User user = findUserByEmail(userEmail);
        List<CartItem> items = cartRepository.findByUser(user);

        List<CartItemResponse> itemResponses = items.stream()
                .map(CartItemResponse::fromEntity)
                .collect(Collectors.toList());

        BigDecimal cartTotal = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemCount = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .items(itemResponses)
                .cartTotal(cartTotal)
                .itemCount(itemCount)
                .build();
    }

    /**
     * Add a product to the cart.
     * If the product already exists in the cart, increase quantity.
     */
    @Transactional
    public CartItemResponse addToCart(String userEmail, AddToCartRequest request) {
        User user = findUserByEmail(userEmail);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        // Validate product is available
        if (product.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient product stock. Available: " + product.getQuantity());
        }

        Optional<CartItem> existing = cartRepository.findByUserAndProduct(user, product);

        CartItem cartItem;
        if (existing.isPresent()) {
            // Update existing cart item — increase quantity
            cartItem = existing.get();
            int newQuantity = cartItem.getQuantity() + request.getQuantity();
            cartItem.setQuantity(Math.min(newQuantity, product.getQuantity()));
            log.info("Cart item updated: userId={}, productId={}, newQty={}",
                    user.getId(), product.getId(), cartItem.getQuantity());
        } else {
            // Create new cart item
            cartItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(Math.min(request.getQuantity(), product.getQuantity()))
                    .build();
            log.info("Cart item added: userId={}, productId={}, qty={}",
                    user.getId(), product.getId(), request.getQuantity());
        }

        CartItem saved = cartRepository.save(cartItem);
        return CartItemResponse.fromEntity(saved);
    }

    /**
     * Update quantity of a cart item
     */
    @Transactional
    public CartItemResponse updateCartItem(String userEmail, UpdateCartRequest request) {
        User user = findUserByEmail(userEmail);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        CartItem cartItem = cartRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "productId", request.getProductId()));

        // Validate quantity
        int newQuantity = Math.min(request.getQuantity(), product.getQuantity());
        cartItem.setQuantity(newQuantity);

        CartItem saved = cartRepository.save(cartItem);
        log.info("Cart item quantity updated: userId={}, productId={}, qty={}",
                user.getId(), product.getId(), newQuantity);
        return CartItemResponse.fromEntity(saved);
    }

    /**
     * Remove a specific product from the cart
     */
    @Transactional
    public void removeCartItem(String userEmail, Long productId) {
        User user = findUserByEmail(userEmail);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        cartRepository.deleteByUserAndProduct(user, product);
        log.info("Cart item removed: userId={}, productId={}", user.getId(), productId);
    }

    /**
     * Clear all items from the user's cart
     */
    @Transactional
    public void clearCart(String userEmail) {
        User user = findUserByEmail(userEmail);
        cartRepository.deleteByUser(user);
        log.info("Cart cleared: userId={}", user.getId());
    }

    /**
     * Get item count for the user's cart (for badge display)
     */
    public long getCartItemCount(String userEmail) {
        User user = findUserByEmail(userEmail);
        return cartRepository.countByUser(user);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
