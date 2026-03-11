package com.krushikranti.repository;

import com.krushikranti.model.AdminLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

    @Query("SELECT l FROM AdminLog l WHERE " +
           "LOWER(l.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.adminName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.target) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<AdminLog> searchLogs(@Param("search") String search, Pageable pageable);

    Page<AdminLog> findByType(String type, Pageable pageable);
}
