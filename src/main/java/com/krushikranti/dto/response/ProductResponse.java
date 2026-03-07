package com.krushikranti.dto.response;

import com.krushikranti.model.Product;
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
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private Integer quantity;
    private String unit;
    private String category;
    private String imageUrl;
    private String location;
    private Boolean organic;
    private String status;
    private Long farmerId;
    private String farmerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .retailPrice(product.getRetailPrice())
                .wholesalePrice(product.getWholesalePrice())
                .quantity(product.getQuantity())
                .unit(product.getUnit())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .location(product.getLocation())
                .organic(product.getOrganic())
                .status(product.getStatus().name())
                .farmerId(product.getFarmer().getId())
                .farmerName(product.getFarmer().getFirstName() + " " + product.getFarmer().getLastName())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
