package com.krushikranti.controller;

import com.krushikranti.dto.request.CreateDealOfferRequest;
import com.krushikranti.dto.request.RespondDealRequest;
import com.krushikranti.dto.request.SendNegotiationMessageRequest;
import com.krushikranti.dto.request.StartNegotiationRequest;
import com.krushikranti.dto.response.*;
import com.krushikranti.service.NegotiationService;
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
@RequestMapping("/api/v1/negotiations")
@RequiredArgsConstructor
@Tag(name = "Negotiations", description = "Endpoints for bulk marketplace negotiation chat system")
@SecurityRequirement(name = "bearerAuth")
public class NegotiationController {

    private final NegotiationService negotiationService;

    @PostMapping("/start")
    @Operation(summary = "Start a negotiation conversation with a farmer")
    @PreAuthorize("hasRole('WHOLESALER')")
    public ResponseEntity<ApiResponse<ConversationResponse>> startConversation(
            @Valid @RequestBody StartNegotiationRequest request,
            Authentication authentication) {

        ConversationResponse response = negotiationService.startConversation(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversation started successfully", response));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Get all conversations for the current user")
    @PreAuthorize("hasAnyRole('FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
            Authentication authentication) {

        List<ConversationResponse> conversations = negotiationService.getConversations(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Conversations fetched successfully", conversations));
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Get messages for a conversation")
    @PreAuthorize("hasAnyRole('FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<NegotiationMessageResponse>>> getMessages(
            @PathVariable Long conversationId,
            Authentication authentication) {

        List<NegotiationMessageResponse> messages = negotiationService.getMessages(authentication.getName(),
                conversationId);
        return ResponseEntity.ok(ApiResponse.success("Messages fetched successfully", messages));
    }

    @PostMapping("/message")
    @Operation(summary = "Send a message in a negotiation (REST fallback)")
    @PreAuthorize("hasAnyRole('FARMER', 'WHOLESALER')")
    public ResponseEntity<ApiResponse<NegotiationMessageResponse>> sendMessage(
            @Valid @RequestBody SendNegotiationMessageRequest request,
            Authentication authentication) {

        NegotiationMessageResponse response = negotiationService.sendMessage(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent successfully", response));
    }

    @PostMapping("/deal")
    @Operation(summary = "Create a deal offer")
    @PreAuthorize("hasAnyRole('FARMER', 'WHOLESALER')")
    public ResponseEntity<ApiResponse<DealOfferResponse>> createDeal(
            @Valid @RequestBody CreateDealOfferRequest request,
            Authentication authentication) {

        DealOfferResponse response = negotiationService.createDealOffer(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Deal offer created successfully", response));
    }

    @PostMapping("/deal/respond")
    @Operation(summary = "Accept or reject a deal offer")
    @PreAuthorize("hasAnyRole('FARMER', 'WHOLESALER')")
    public ResponseEntity<ApiResponse<DealOfferResponse>> respondToDeal(
            @Valid @RequestBody RespondDealRequest request,
            Authentication authentication) {

        DealOfferResponse response = negotiationService.respondToDeal(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Deal response recorded", response));
    }

    @GetMapping("/{conversationId}/deals")
    @Operation(summary = "Get deal offers for a conversation")
    @PreAuthorize("hasAnyRole('FARMER', 'WHOLESALER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DealOfferResponse>>> getDeals(
            @PathVariable Long conversationId,
            Authentication authentication) {

        List<DealOfferResponse> deals = negotiationService.getDeals(authentication.getName(), conversationId);
        return ResponseEntity.ok(ApiResponse.success("Deals fetched successfully", deals));
    }

    @PostMapping("/{conversationId}/mark-read")
    @Operation(summary = "Mark all messages in a conversation as read")
    @PreAuthorize("hasAnyRole('FARMER', 'WHOLESALER')")
    public ResponseEntity<ApiResponse<Void>> markMessagesAsRead(
            @PathVariable Long conversationId,
            Authentication authentication) {

        negotiationService.markMessagesAsRead(authentication.getName(), conversationId);
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }
}
