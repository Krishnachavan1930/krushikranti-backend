package com.krushikranti.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProductRequest {

    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Retail price must be greater than 0")
    private BigDecimal retailPrice;

    @DecimalMin(value = "0.01", message = "Wholesale price must be greater than 0")
    private BigDecimal wholesalePrice;

    @Min(value = 0, message = "Quantity must be at least 0")
    private Integer quantity;

    private String unit;

    private String category;

    private String imageUrl;

    private String location;

    private Boolean organic;

    private String status; // ACTIVE, SOLD_OUT, INACTIVE
}
