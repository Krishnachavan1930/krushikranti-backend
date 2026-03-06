package com.krushikranti.dto.response;

import com.krushikranti.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private Long senderId;
    private Long receiverId;
    private String message;
    private LocalDateTime timestamp;

    public static ChatMessageResponse fromEntity(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .id(chatMessage.getId())
                .senderId(chatMessage.getSender().getId())
                .receiverId(chatMessage.getReceiver().getId())
                .message(chatMessage.getMessage())
                .timestamp(chatMessage.getTimestamp())
                .build();
    }
}
