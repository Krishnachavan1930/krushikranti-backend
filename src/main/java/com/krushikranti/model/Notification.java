package com.krushikranti.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    // Optional reference to related entity
    private Long referenceId;
    private String referenceType;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum NotificationType {
        ORDER_PLACED,
        ORDER_CONFIRMED,
        ORDER_SHIPPED,
        ORDER_DELIVERED,
        ORDER_CANCELLED,
        PAYMENT_RECEIVED,
        PRODUCT_APPROVED,
        PRODUCT_REJECTED,
        NEW_REVIEW,
        PRICE_NEGOTIATION,
        SYSTEM
    }
}
