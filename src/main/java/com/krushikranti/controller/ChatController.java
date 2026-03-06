package com.krushikranti.controller;

import com.krushikranti.dto.request.SendMessageRequest;
import com.krushikranti.dto.response.ApiResponse;
import com.krushikranti.dto.response.ChatMessageResponse;
import com.krushikranti.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Endpoints for messaging between FARMER and WHOLESALER")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    @Operation(summary = "Send a chat message")
    @PreAuthorize("hasAnyRole('FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {

        ChatMessageResponse response = chatService.sendMessage(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent successfully", response));
    }

    @GetMapping("/conversation")
    @Operation(summary = "Get conversation with a specific user")
    @PreAuthorize("hasAnyRole('FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getConversation(
            @RequestParam Long userId,
            Authentication authentication) {

        List<ChatMessageResponse> conversation = chatService.getConversation(authentication.getName(), userId);
        return ResponseEntity.ok(ApiResponse.success("Conversation fetched successfully", conversation));
    }
}
