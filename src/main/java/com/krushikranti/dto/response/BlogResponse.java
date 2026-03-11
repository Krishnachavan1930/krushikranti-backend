package com.krushikranti.dto.response;

import com.krushikranti.model.Blog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlogResponse {

    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String imageUrl;
    private String category;
    private String tags;
    private String authorName;
    private Long authorId;
    private String status;
    private String metaTitle;
    private String metaDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BlogResponse fromEntity(Blog blog) {
        return BlogResponse.builder()
                .id(blog.getId())
                .title(blog.getTitle())
                .slug(blog.getSlug())
                .excerpt(blog.getExcerpt())
                .content(blog.getContent())
                .imageUrl(blog.getImageUrl())
                .category(blog.getCategory())
                .tags(blog.getTags())
                .authorName(blog.getAuthorName() != null
                        ? blog.getAuthorName()
                        : (blog.getAuthor() != null ? blog.getAuthor().getName() : ""))
                .authorId(blog.getAuthor() != null ? blog.getAuthor().getId() : null)
                .status(blog.getStatus() != null ? blog.getStatus().name() : Blog.BlogStatus.DRAFT.name())
                .metaTitle(blog.getMetaTitle())
                .metaDescription(blog.getMetaDescription())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .build();
    }
}
