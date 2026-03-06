package com.krushikranti.service;

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

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    public Page<Blog> getAllBlogs(String search, Pageable pageable) {
        return blogRepository.findWithFilters(Blog.BlogStatus.PUBLISHED, search, pageable);
    }

    public Page<Blog> getPublishedBlogs(Pageable pageable) {
        return blogRepository.findPublishedBlogs(pageable);
    }

    public Blog getBlogById(Long id) {
        return blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog", "id", id));
    }

    public Page<Blog> getBlogsByAuthor(Long authorId, Pageable pageable) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId));
        return blogRepository.findByAuthor(author, pageable);
    }

    @Transactional
    public Blog createBlog(Blog blog, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId));
        blog.setAuthor(author);
        return blogRepository.save(blog);
    }

    @Transactional
    public Blog updateBlog(Long id, Blog updatedBlog) {
        Blog existing = getBlogById(id);
        existing.setTitle(updatedBlog.getTitle());
        existing.setContent(updatedBlog.getContent());
        existing.setImageUrl(updatedBlog.getImageUrl());
        if (updatedBlog.getStatus() != null) {
            existing.setStatus(updatedBlog.getStatus());
        }
        return blogRepository.save(existing);
    }

    @Transactional
    public void deleteBlog(Long id) {
        Blog blog = getBlogById(id);
        blogRepository.delete(blog);
    }

    @Transactional
    public Blog publishBlog(Long id) {
        Blog blog = getBlogById(id);
        blog.setStatus(Blog.BlogStatus.PUBLISHED);
        return blogRepository.save(blog);
    }

    @Transactional
    public Blog archiveBlog(Long id) {
        Blog blog = getBlogById(id);
        blog.setStatus(Blog.BlogStatus.ARCHIVED);
        return blogRepository.save(blog);
    }
}
