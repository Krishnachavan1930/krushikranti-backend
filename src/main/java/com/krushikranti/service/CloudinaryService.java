package com.krushikranti.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.krushikranti.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Upload an image to Cloudinary
     * @param file The image file to upload
     * @param folder The folder name in Cloudinary (e.g., "products", "avatars", "blogs")
     * @return The URL of the uploaded image
     */
    public String uploadImage(MultipartFile file, String folder) {
        validateFile(file);
        
        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", "krushikranti/" + folder,
                    "resource_type", "image",
                    "transformation", getTransformation(folder)
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            
            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Image uploaded successfully to Cloudinary: {}", secureUrl);
            
            return secureUrl;
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new FileUploadException("Failed to upload image: " + e.getMessage());
        }
    }

    /**
     * Delete an image from Cloudinary by its public ID
     * @param publicId The public ID of the image to delete
     */
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted successfully from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary", e);
            throw new FileUploadException("Failed to delete image: " + e.getMessage());
        }
    }

    /**
     * Extract public ID from Cloudinary URL
     */
    public String extractPublicId(String cloudinaryUrl) {
        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
            return null;
        }
        // URL format: https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/{public_id}.{format}
        String[] parts = cloudinaryUrl.split("/upload/");
        if (parts.length < 2) {
            return null;
        }
        String pathWithVersion = parts[1];
        // Remove version (v1234567890/)
        String pathWithoutVersion = pathWithVersion.replaceFirst("v\\d+/", "");
        // Remove file extension
        int lastDot = pathWithoutVersion.lastIndexOf('.');
        if (lastDot > 0) {
            return pathWithoutVersion.substring(0, lastDot);
        }
        return pathWithoutVersion;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is empty or not provided");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException("File size exceeds maximum allowed size of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new FileUploadException("Invalid file type. Allowed types: JPEG, PNG, GIF, WEBP");
        }
    }

    private Map<String, Object> getTransformation(String folder) {
        return switch (folder) {
            case "avatars" -> ObjectUtils.asMap(
                    "width", 200,
                    "height", 200,
                    "crop", "fill",
                    "gravity", "face",
                    "quality", "auto"
            );
            case "products" -> ObjectUtils.asMap(
                    "width", 800,
                    "height", 800,
                    "crop", "limit",
                    "quality", "auto"
            );
            case "blogs" -> ObjectUtils.asMap(
                    "width", 1200,
                    "height", 630,
                    "crop", "fill",
                    "quality", "auto"
            );
            default -> ObjectUtils.asMap(
                    "quality", "auto"
            );
        };
    }
}
