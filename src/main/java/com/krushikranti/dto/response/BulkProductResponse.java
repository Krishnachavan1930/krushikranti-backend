package com.krushikranti.dto.response;

import com.krushikranti.model.BulkProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkProductResponse {

    private Long id;
    private Long farmerId;
    private String farmerName;
    private String name;
    private String description;
    private Integer quantity;
    private BigDecimal minimumPrice;
    private String location;
    private String imageUrl;
    private String status;
    private LocalDateTime createdAt;

    public static BulkProductResponse fromEntity(BulkProduct bp) {
        return BulkProductResponse.builder()
                .id(bp.getId())
                .farmerId(bp.getFarmer().getId())
                .farmerName(bp.getFarmer().getName())
                .name(bp.getName())
                .description(bp.getDescription())
                .quantity(bp.getQuantity())
                .minimumPrice(bp.getMinimumPrice())
                .location(bp.getLocation())
                .imageUrl(bp.getImageUrl())
                .status(bp.getStatus().name())
                .createdAt(bp.getCreatedAt())
                .build();
    }
}
