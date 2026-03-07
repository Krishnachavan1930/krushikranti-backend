package com.krushikranti.controller;

import com.krushikranti.dto.request.SendNegotiationMessageRequest;
import com.krushikranti.dto.response.NegotiationMessageResponse;
import com.krushikranti.service.NegotiationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class NegotiationWebSocketController {

    private final NegotiationService negotiationService;

    /**
     * Handles STOMP messages sent to /app/negotiation.send
     * Saves the message and broadcasts to /topic/negotiation/{conversationId}
     */
    @MessageMapping("/negotiation.send")
    public void handleNegotiationMessage(
            @Payload SendNegotiationMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            log.warn("WebSocket message received without authentication");
            return;
        }

        try {
            NegotiationMessageResponse response = negotiationService.sendMessage(principal.getName(), request);
            log.debug("WebSocket message processed for conversation: {}", request.getConversationId());
        } catch (Exception e) {
            log.error("Error processing WebSocket message: {}", e.getMessage(), e);
        }
    }
}
