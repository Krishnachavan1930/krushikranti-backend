package com.krushikranti.dto.response;

import com.krushikranti.model.CartItem;
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
public class CartItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private String category;
    private BigDecimal price;
    private BigDecimal wholesalePrice;
    private Integer quantity;
    private String unit;
    private Integer maxStock;
    private BigDecimal subtotal;
    private LocalDateTime createdAt;

    public static CartItemResponse fromEntity(CartItem cartItem) {
        BigDecimal price = cartItem.getProduct().getRetailPrice();
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProduct().getId())
                .productName(cartItem.getProduct().getName())
                .productImage(cartItem.getProduct().getImageUrl())
                .category(cartItem.getProduct().getCategory())
                .price(price)
                .wholesalePrice(cartItem.getProduct().getWholesalePrice())
                .quantity(cartItem.getQuantity())
                .unit(cartItem.getProduct().getUnit())
                .maxStock(cartItem.getProduct().getQuantity())
                .subtotal(price.multiply(new BigDecimal(cartItem.getQuantity())))
                .createdAt(cartItem.getCreatedAt())
                .build();
    }
}
