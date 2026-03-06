package com.krushikranti.controller;

import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.UploadResponse;
import com.krushikranti.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "Endpoints for uploading images to Cloudinary")
@SecurityRequirement(name = "bearerAuth")
public class UploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload an image",
            description = "Upload an image to Cloudinary. Supported types: JPEG, PNG, GIF, WEBP. Max size: 10MB."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadImage(
            @Parameter(description = "The image file to upload")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Upload type: 'products', 'avatars', or 'blogs'")
            @RequestParam(value = "type", defaultValue = "products") String type) {

        // Validate upload type
        String folder = validateAndGetFolder(type);
        
        // Upload to Cloudinary
        String imageUrl = cloudinaryService.uploadImage(file, folder);
        String publicId = cloudinaryService.extractPublicId(imageUrl);

        UploadResponse response = UploadResponse.builder()
                .url(imageUrl)
                .publicId(publicId)
                .folder(folder)
                .originalFilename(file.getOriginalFilename())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", response));
    }

    @PostMapping(value = "/product-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a product image")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadProductImage(
            @RequestParam("file") MultipartFile file) {
        return uploadImage(file, "products");
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a user avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        return uploadImage(file, "avatars");
    }

    @PostMapping(value = "/blog-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a blog image")
    @PreAuthorize("hasAnyRole('ADMIN', 'FARMER')")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadBlogImage(
            @RequestParam("file") MultipartFile file) {
        return uploadImage(file, "blogs");
    }

    @DeleteMapping("/image")
    @Operation(summary = "Delete an image from Cloudinary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @Parameter(description = "The public ID of the image to delete")
            @RequestParam("publicId") String publicId) {
        cloudinaryService.deleteImage(publicId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully", null));
    }

    private String validateAndGetFolder(String type) {
        return switch (type.toLowerCase()) {
            case "products", "product" -> "products";
            case "avatars", "avatar" -> "avatars";
            case "blogs", "blog" -> "blogs";
            default -> "general";
        };
    }
}
