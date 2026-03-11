package com.krushikranti.controller;

import com.krushikranti.dto.request.BlogRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.BlogResponse;
import com.krushikranti.service.BlogService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
@Tag(name = "Blogs", description = "Blog post management endpoints")
public class BlogController {

    private final BlogService blogService;

    // ──────────────────────────────────────────────────────────────────
    // PUBLIC endpoints (no auth required — SecurityConfig permits GET /api/blogs/**)
    // ──────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get published blogs (public)")
    public ResponseEntity<ApiResponse<Page<BlogResponse>>> getPublishedBlogs(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success("Blogs fetched", blogService.getPublishedBlogs(search, pageable)));
    }

    // ──────────────────────────────────────────────────────────────────
    // ADMIN endpoints (all statuses)
    // ──────────────────────────────────────────────────────────────────

    @GetMapping("/admin/all")
    @Operation(summary = "Get all blogs for admin (any status)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BlogResponse>>> getAllBlogsAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success("All blogs fetched", blogService.getAllBlogsAdmin(search, pageable)));
    }

    @GetMapping("/admin/stats")
    @Operation(summary = "Get blog stats (total / published / draft / archived)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getBlogStats() {
        return ResponseEntity.ok(ApiResponse.success("Blog stats fetched", blogService.getBlogStats()));
    }

    @GetMapping("/admin/{id}")
    @Operation(summary = "Get blog by ID (admin use)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponse>> getBlogById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Blog fetched", blogService.getBlogById(id)));
    }

    @PostMapping
    @Operation(summary = "Create blog post")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponse>> createBlog(
            @RequestBody BlogRequest req,
            @RequestParam Long authorId) {
        BlogResponse created = blogService.createBlog(req, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Blog created", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update blog post")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponse>> updateBlog(
            @PathVariable Long id,
            @RequestBody BlogRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Blog updated", blogService.updateBlog(id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete blog post")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBlog(@PathVariable Long id) {
        blogService.deleteBlog(id);
        return ResponseEntity.ok(ApiResponse.success("Blog deleted", null));
    }

    @PatchMapping("/{id}/publish")
    @Operation(summary = "Publish blog post")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponse>> publishBlog(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Blog published", blogService.publishBlog(id)));
    }

    @PatchMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish blog post (revert to DRAFT)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponse>> unpublishBlog(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Blog unpublished", blogService.unpublishBlog(id)));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive blog post")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponse>> archiveBlog(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Blog archived", blogService.archiveBlog(id)));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get published blog by slug (public)")
    public ResponseEntity<ApiResponse<BlogResponse>> getBlogBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success("Blog fetched", blogService.getBlogBySlug(slug)));
    }
}

