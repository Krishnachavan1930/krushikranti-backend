package com.krushikranti.service;

import com.krushikranti.dto.request.CreateProductRequest;
import com.krushikranti.dto.request.UpdateProductRequest;
import com.krushikranti.dto.response.ProductResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.Product;
import com.krushikranti.model.User;
import com.krushikranti.repository.ProductRepository;
import com.krushikranti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Get all active products with optional category/search filters (public)
     */
    public Page<ProductResponse> getAllProducts(String category, String search, Pageable pageable) {
        Page<Product> products = productRepository.findWithFilters(
                category, Product.ProductStatus.ACTIVE, search, pageable);
        return products.map(ProductResponse::fromEntity);
    }

    /**
     * Get a single product by ID (public)
     */
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return ProductResponse.fromEntity(product);
    }

    /**
     * Get all products for a specific farmer by farmer ID
     */
    public Page<ProductResponse> getProductsByFarmer(Long farmerId, Pageable pageable) {
        userRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", farmerId));
        Page<Product> products = productRepository.findByFarmerId(farmerId, pageable);
        return products.map(ProductResponse::fromEntity);
    }

    /**
     * Get products for the currently logged-in farmer (by email)
     */
    public Page<ProductResponse> getMyProducts(String farmerEmail, Pageable pageable) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", farmerEmail));
        Page<Product> products = productRepository.findByFarmer(farmer, pageable);
        return products.map(ProductResponse::fromEntity);
    }

    /**
     * Create a new product — optionally upload an image file or accept an imageUrl
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request, String farmerEmail,
            MultipartFile imageFile) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", farmerEmail));

        // Determine image URL — uploaded file takes priority over pasted URL
        String imageUrl = resolveImageUrl(imageFile, request.getImageUrl());

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .retailPrice(request.getRetailPrice())
                .wholesalePrice(request.getWholesalePrice())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .category(request.getCategory())
                .imageUrl(imageUrl)
                .location(request.getLocation())
                .organic(request.getOrganic() != null ? request.getOrganic() : false)
                .status(Product.ProductStatus.ACTIVE)
                .farmer(farmer)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created: id={}, name={}, farmer={}", saved.getId(), saved.getName(), farmerEmail);
        return ProductResponse.fromEntity(saved);
    }

    /**
     * Update an existing product — only the owning farmer or ADMIN can update
     */
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request,
            String userEmail, MultipartFile imageFile) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Verify ownership
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        boolean isAdmin = user.getRole() == User.Role.ROLE_ADMIN;
        if (!isAdmin && !existing.getFarmer().getId().equals(user.getId())) {
            throw new SecurityException("You can only update your own products");
        }

        // Update fields if provided
        if (request.getName() != null)
            existing.setName(request.getName());
        if (request.getDescription() != null)
            existing.setDescription(request.getDescription());
        if (request.getRetailPrice() != null)
            existing.setRetailPrice(request.getRetailPrice());
        if (request.getWholesalePrice() != null)
            existing.setWholesalePrice(request.getWholesalePrice());
        if (request.getQuantity() != null)
            existing.setQuantity(request.getQuantity());
        if (request.getUnit() != null)
            existing.setUnit(request.getUnit());
        if (request.getCategory() != null)
            existing.setCategory(request.getCategory());
        if (request.getLocation() != null)
            existing.setLocation(request.getLocation());
        if (request.getOrganic() != null)
            existing.setOrganic(request.getOrganic());
        if (request.getStatus() != null) {
            try {
                existing.setStatus(Product.ProductStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid product status: {}", request.getStatus());
            }
        }

        // Handle image update
        String newImageUrl = resolveImageUrl(imageFile, request.getImageUrl());
        if (newImageUrl != null) {
            existing.setImageUrl(newImageUrl);
        }

        Product saved = productRepository.save(existing);
        log.info("Product updated: id={}, name={}", saved.getId(), saved.getName());
        return ProductResponse.fromEntity(saved);
    }

    /**
     * Delete a product — only the owning farmer or ADMIN can delete
     */
    @Transactional
    public void deleteProduct(Long id, String userEmail) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        boolean isAdmin = user.getRole() == User.Role.ROLE_ADMIN;
        if (!isAdmin && !product.getFarmer().getId().equals(user.getId())) {
            throw new SecurityException("You can only delete your own products");
        }

        // Delete image from Cloudinary if it's a Cloudinary URL
        if (product.getImageUrl() != null && product.getImageUrl().contains("cloudinary")) {
            try {
                String publicId = cloudinaryService.extractPublicId(product.getImageUrl());
                if (publicId != null) {
                    cloudinaryService.deleteImage(publicId);
                }
            } catch (Exception e) {
                log.warn("Failed to delete image from Cloudinary: {}", e.getMessage());
            }
        }

        productRepository.delete(product);
        log.info("Product deleted: id={}, name={}", id, product.getName());
    }

    /**
     * Resolve the final image URL. Uploaded file takes priority over pasted URL.
     */
    private String resolveImageUrl(MultipartFile imageFile, String pastedUrl) {
        // If a file is uploaded, upload to Cloudinary and return the URL
        if (imageFile != null && !imageFile.isEmpty()) {
            return cloudinaryService.uploadImage(imageFile, "products");
        }
        // Otherwise use the pasted URL (may be null)
        return pastedUrl;
    }
}
