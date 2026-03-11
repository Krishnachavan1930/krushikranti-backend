package com.krushikranti.service;

import com.krushikranti.dto.request.BlogRequest;
import com.krushikranti.dto.response.BlogResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.Blog;
import com.krushikranti.model.User;
import com.krushikranti.repository.BlogRepository;
import com.krushikranti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    // ─────────────────────────────────────────────────────────────────
    // Public (published only)
    // ─────────────────────────────────────────────────────────────────

    public Page<BlogResponse> getPublishedBlogs(String search, Pageable pageable) {
        return blogRepository
                .findWithFilters(Blog.BlogStatus.PUBLISHED, search, pageable)
                .map(BlogResponse::fromEntity);
    }

    public BlogResponse getBlogBySlug(String slug) {
        Blog blog = blogRepository.findBySlug(slug).orElseGet(() -> {
            try {
                Long id = Long.parseLong(slug);
                return blogRepository.findById(id).orElse(null);
            } catch (NumberFormatException ignored) {
                return null;
            }
        });
        if (blog == null) {
            throw new ResourceNotFoundException("Blog", "slug", slug);
        }
        if (blog.getStatus() != Blog.BlogStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Blog", "slug", slug);
        }
        return BlogResponse.fromEntity(blog);
    }

    // ─────────────────────────────────────────────────────────────────
    // Admin (all statuses)
    // ─────────────────────────────────────────────────────────────────

    public Page<BlogResponse> getAllBlogsAdmin(String search, Pageable pageable) {
        return blogRepository.findAllWithSearch(search, pageable).map(BlogResponse::fromEntity);
    }

    public BlogResponse getBlogById(Long id) {
        return BlogResponse.fromEntity(
                blogRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Blog", "id", id)));
    }

    public Map<String, Long> getBlogStats() {
        long total = blogRepository.count();
        long published = blogRepository.countByStatus(Blog.BlogStatus.PUBLISHED);
        long draft = blogRepository.countByStatus(Blog.BlogStatus.DRAFT);
        long archived = blogRepository.countByStatus(Blog.BlogStatus.ARCHIVED);
        return Map.of("total", total, "published", published, "draft", draft, "archived", archived);
    }

    @Transactional
    public BlogResponse createBlog(BlogRequest req, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId));

        String slug = req.getSlug() != null && !req.getSlug().isBlank()
                ? slugify(req.getSlug())
                : slugify(req.getTitle());
        slug = uniqueSlug(slug);

        Blog.BlogStatus status = parseStatus(req.getStatus());

        Blog blog = Blog.builder()
                .title(req.getTitle())
                .slug(slug)
                .excerpt(req.getExcerpt())
                .content(req.getContent())
                .imageUrl(req.getImageUrl())
                .category(req.getCategory())
                .tags(req.getTags())
                .authorName(req.getAuthorName() != null && !req.getAuthorName().isBlank()
                        ? req.getAuthorName() : author.getName())
                .author(author)
                .status(status)
                .metaTitle(req.getMetaTitle())
                .metaDescription(req.getMetaDescription())
                .build();

        return BlogResponse.fromEntity(blogRepository.save(blog));
    }

    @Transactional
    public BlogResponse updateBlog(Long id, BlogRequest req) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", "id", id));

        blog.setTitle(req.getTitle());
        blog.setExcerpt(req.getExcerpt());
        blog.setContent(req.getContent());
        if (req.getImageUrl() != null) blog.setImageUrl(req.getImageUrl());
        if (req.getCategory() != null) blog.setCategory(req.getCategory());
        if (req.getTags() != null) blog.setTags(req.getTags());
        if (req.getAuthorName() != null) blog.setAuthorName(req.getAuthorName());
        if (req.getMetaTitle() != null) blog.setMetaTitle(req.getMetaTitle());
        if (req.getMetaDescription() != null) blog.setMetaDescription(req.getMetaDescription());

        // Update slug only if explicitly provided and different
        if (req.getSlug() != null && !req.getSlug().isBlank()) {
            String newSlug = slugify(req.getSlug());
            if (!newSlug.equals(blog.getSlug()) && !blogRepository.existsBySlug(newSlug)) {
                blog.setSlug(newSlug);
            }
        }

        if (req.getStatus() != null) blog.setStatus(parseStatus(req.getStatus()));

        return BlogResponse.fromEntity(blogRepository.save(blog));
    }

    @Transactional
    public void deleteBlog(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", "id", id));
        blogRepository.delete(blog);
    }

    @Transactional
    public BlogResponse publishBlog(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", "id", id));
        blog.setStatus(Blog.BlogStatus.PUBLISHED);
        return BlogResponse.fromEntity(blogRepository.save(blog));
    }

    @Transactional
    public BlogResponse unpublishBlog(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", "id", id));
        blog.setStatus(Blog.BlogStatus.DRAFT);
        return BlogResponse.fromEntity(blogRepository.save(blog));
    }

    @Transactional
    public BlogResponse archiveBlog(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", "id", id));
        blog.setStatus(Blog.BlogStatus.ARCHIVED);
        return BlogResponse.fromEntity(blogRepository.save(blog));
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(
                WHITESPACE.matcher(normalized.toLowerCase(Locale.ENGLISH)).replaceAll("-")
        ).replaceAll("");
        return slug.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }

    private String uniqueSlug(String base) {
        if (!blogRepository.existsBySlug(base)) return base;
        int suffix = 1;
        while (blogRepository.existsBySlug(base + "-" + suffix)) suffix++;
        return base + "-" + suffix;
    }

    private Blog.BlogStatus parseStatus(String s) {
        if (s == null) return Blog.BlogStatus.DRAFT;
        try {
            return Blog.BlogStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Blog.BlogStatus.DRAFT;
        }
    }
}
