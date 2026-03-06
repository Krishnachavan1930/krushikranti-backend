package com.krushikranti.repository;

import com.krushikranti.model.Blog;
import com.krushikranti.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {

    Page<Blog> findByAuthor(User author, Pageable pageable);

    Page<Blog> findByStatus(Blog.BlogStatus status, Pageable pageable);

    List<Blog> findByAuthorId(Long authorId);

    @Query("SELECT b FROM Blog b WHERE " +
            "(:status IS NULL OR b.status = :status) AND " +
            "(:search IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Blog> findWithFilters(
            @Param("status") Blog.BlogStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT b FROM Blog b WHERE b.status = 'PUBLISHED' ORDER BY b.createdAt DESC")
    Page<Blog> findPublishedBlogs(Pageable pageable);
}
