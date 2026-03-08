package com.krushikranti.service;

import com.krushikranti.dto.request.FarmerRatingRequest;
import com.krushikranti.dto.response.FarmerRatingResponse;
import com.krushikranti.exception.BadRequestException;
import com.krushikranti.exception.DuplicateResourceException;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.BulkOrder;
import com.krushikranti.model.FarmerRating;
import com.krushikranti.model.Notification;
import com.krushikranti.model.User;
import com.krushikranti.repository.BulkOrderRepository;
import com.krushikranti.repository.FarmerRatingRepository;
import com.krushikranti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerRatingService {

    private final FarmerRatingRepository farmerRatingRepository;
    private final BulkOrderRepository bulkOrderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public FarmerRatingResponse submitRating(String wholesalerEmail, FarmerRatingRequest request) {
        User wholesaler = userRepository.findByEmail(wholesalerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Wholesaler not found"));

        BulkOrder order = bulkOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Only the wholesaler of this order can rate
        if (!order.getWholesaler().getId().equals(wholesaler.getId())) {
            throw new BadRequestException("You can only rate farmers for your own orders");
        }

        // Order must be delivered
        if (order.getDeliveryStatus() != BulkOrder.DeliveryStatus.DELIVERED) {
            throw new BadRequestException("You can only rate a farmer after the order is delivered");
        }

        // One rating per order
        if (farmerRatingRepository.existsByWholesalerIdAndOrderId(wholesaler.getId(), order.getId())) {
            throw new DuplicateResourceException("You have already rated this farmer for this order");
        }

        FarmerRating rating = FarmerRating.builder()
                .farmer(order.getFarmer())
                .wholesaler(wholesaler)
                .order(order)
                .rating(request.getRating())
                .review(request.getReview())
                .build();

        FarmerRating saved = farmerRatingRepository.save(rating);

        // Notify the farmer
        notificationService.createNotification(
                order.getFarmer(),
                "New Rating Received",
                wholesaler.getName() + " gave you a " + request.getRating() + "★ rating",
                Notification.NotificationType.NEW_REVIEW,
                saved.getId(),
                "FARMER_RATING"
        );

        log.info("Rating {} submitted for farmer {} by wholesaler {}", request.getRating(),
                order.getFarmer().getId(), wholesaler.getId());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FarmerRatingResponse> getFarmerRatings(Long farmerId) {
        return farmerRatingRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFarmerRatingStats(Long farmerId) {
        Double avg = farmerRatingRepository.findAverageRatingByFarmerId(farmerId);
        Long count = farmerRatingRepository.countByFarmerId(farmerId);
        return Map.of(
                "averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0,
                "totalRatings", count
        );
    }

    private FarmerRatingResponse toResponse(FarmerRating r) {
        return FarmerRatingResponse.builder()
                .id(r.getId())
                .farmerId(r.getFarmer().getId())
                .farmerName(r.getFarmer().getName())
                .wholesalerId(r.getWholesaler().getId())
                .wholesalerName(r.getWholesaler().getName())
                .orderId(r.getOrder().getId())
                .rating(r.getRating())
                .review(r.getReview())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
