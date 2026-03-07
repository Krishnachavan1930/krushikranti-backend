package com.krushikranti.dto.response;

import com.krushikranti.model.DealOffer;
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
public class DealOfferResponse {

    private Long id;
    private Long conversationId;
    private Long createdById;
    private String createdByName;
    private BigDecimal pricePerUnit;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createdAt;

    public static DealOfferResponse fromEntity(DealOffer deal) {
        return DealOfferResponse.builder()
                .id(deal.getId())
                .conversationId(deal.getConversation().getId())
                .createdById(deal.getCreatedBy().getId())
                .createdByName(deal.getCreatedBy().getName())
                .pricePerUnit(deal.getPricePerUnit())
                .quantity(deal.getQuantity())
                .totalPrice(deal.getTotalPrice())
                .status(deal.getStatus().name())
                .createdAt(deal.getCreatedAt())
                .build();
    }
}
