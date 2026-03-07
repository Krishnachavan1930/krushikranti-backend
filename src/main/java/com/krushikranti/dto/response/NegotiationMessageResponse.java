package com.krushikranti.dto.response;

import com.krushikranti.model.NegotiationMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NegotiationMessageResponse {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String message;
    private String messageType;
    private String status;
    private LocalDateTime createdAt;

    public static NegotiationMessageResponse fromEntity(NegotiationMessage msg) {
        return NegotiationMessageResponse.builder()
                .id(msg.getId())
                .conversationId(msg.getConversation().getId())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getName())
                .message(msg.getMessage())
                .messageType(msg.getMessageType().name())
                .status(msg.getStatus().name())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
