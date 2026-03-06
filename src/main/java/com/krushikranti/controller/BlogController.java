package com.krushikranti.controller;

import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.model.Blog;
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

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
@Tag(name = "Blogs", description = "Endpoints for managing blog posts")
public class BlogController {

    private final BlogService blogService;

    @GetMapping
    @Operation(summary = "Get all published blogs", description = "Returns paginated list of published blogs")
    public ResponseEntity<ApiResponse<Page<Blog>>> getAllBlogs(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Blog> blogs = blogService.getAllBlogs(search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Blogs fetched successfully", blogs));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get blog by ID")
    public ResponseEntity<ApiResponse<Blog>> getBlogById(@PathVariable Long id) {
        Blog blog = blogService.getBlogById(id);
        return ResponseEntity.ok(ApiResponse.success("Blog fetched successfully", blog));
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "Get blogs by author")
    public ResponseEntity<ApiResponse<Page<Blog>>> getBlogsByAuthor(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Blog> blogs = blogService.getBlogsByAuthor(authorId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Author blogs fetched successfully", blogs));
    }

    @PostMapping
    @Operation(summary = "Create a blog post")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Blog>> createBlog(
            @RequestBody Blog blog,
            @RequestParam Long authorId) {
        Blog created = blogService.createBlog(blog, authorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Blog created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a blog post")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Blog>> updateBlog(
            @PathVariable Long id,
            @RequestBody Blog blog) {
        Blog updated = blogService.updateBlog(id, blog);
        return ResponseEntity.ok(ApiResponse.success("Blog updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a blog post")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBlog(@PathVariable Long id) {
        blogService.deleteBlog(id);
        return ResponseEntity.ok(ApiResponse.success("Blog deleted successfully", null));
    }

    @PatchMapping("/{id}/publish")
    @Operation(summary = "Publish a blog post")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Blog>> publishBlog(@PathVariable Long id) {
        Blog published = blogService.publishBlog(id);
        return ResponseEntity.ok(ApiResponse.success("Blog published successfully", published));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive a blog post")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Blog>> archiveBlog(@PathVariable Long id) {
        Blog archived = blogService.archiveBlog(id);
        return ResponseEntity.ok(ApiResponse.success("Blog archived successfully", archived));
    }
}
