package com.krushikranti.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FarmerRatingResponse {
    private Long id;
    private Long farmerId;
    private String farmerName;
    private Long wholesalerId;
    private String wholesalerName;
    private Long orderId;
    private int rating;
    private String review;
    private LocalDateTime createdAt;
}
