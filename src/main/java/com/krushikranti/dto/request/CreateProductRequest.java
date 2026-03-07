package com.krushikranti.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Retail price is required")
    @DecimalMin(value = "0.01", message = "Retail price must be greater than 0")
    private BigDecimal retailPrice;

    @DecimalMin(value = "0.01", message = "Wholesale price must be greater than 0")
    private BigDecimal wholesalePrice;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be at least 0")
    private Integer quantity;

    @NotBlank(message = "Unit is required")
    private String unit;

    @NotBlank(message = "Category is required")
    private String category;

    private String imageUrl;

    private String location;

    @Builder.Default
    private Boolean organic = false;
}
