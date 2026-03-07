package com.krushikranti.dto.response;

import com.krushikranti.model.Conversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationResponse {

    private Long id;
    private Long bulkProductId;
    private String bulkProductName;
    private Long farmerId;
    private String farmerName;
    private Long wholesalerId;
    private String wholesalerName;
    private String status;
    private LocalDateTime createdAt;

    public static ConversationResponse fromEntity(Conversation conv) {
        return ConversationResponse.builder()
                .id(conv.getId())
                .bulkProductId(conv.getBulkProduct().getId())
                .bulkProductName(conv.getBulkProduct().getName())
                .farmerId(conv.getFarmer().getId())
                .farmerName(conv.getFarmer().getName())
                .wholesalerId(conv.getWholesaler().getId())
                .wholesalerName(conv.getWholesaler().getName())
                .status(conv.getStatus().name())
                .createdAt(conv.getCreatedAt())
                .build();
    }
}
