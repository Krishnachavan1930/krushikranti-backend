package com.krushikranti.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingEvent {
    private Long conversationId;
    private Long senderId;
    private boolean typing;
}
