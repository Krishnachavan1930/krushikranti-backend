package com.krushikranti.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "farmer_ratings",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_rating_per_order",
        columnNames = {"wholesaler_id", "order_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmerRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wholesaler_id", nullable = false)
    private User wholesaler;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private BulkOrder order;

    @Column(nullable = false)
    private int rating; // 1-5

    @Column(columnDefinition = "TEXT")
    private String review;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
