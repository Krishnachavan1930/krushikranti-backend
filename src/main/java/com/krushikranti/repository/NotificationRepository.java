package com.krushikranti.repository;

import com.krushikranti.model.Notification;
import com.krushikranti.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    long countByUserAndIsReadFalse(User user);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user")
    void markAllAsReadForUser(@Param("user") User user);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user = :user")
    void markAsRead(@Param("id") Long id, @Param("user") User user);

    void deleteByUser(User user);
}
