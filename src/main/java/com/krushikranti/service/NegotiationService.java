package com.krushikranti.service;

import com.krushikranti.dto.request.CreateDealOfferRequest;
import com.krushikranti.dto.request.RespondDealRequest;
import com.krushikranti.dto.request.SendNegotiationMessageRequest;
import com.krushikranti.dto.request.StartNegotiationRequest;
import com.krushikranti.dto.response.ConversationResponse;
import com.krushikranti.dto.response.DealOfferResponse;
import com.krushikranti.dto.response.NegotiationMessageResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.*;
import com.krushikranti.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NegotiationService {

    private final ConversationRepository conversationRepository;
    private final NegotiationMessageRepository messageRepository;
    private final DealOfferRepository dealOfferRepository;
    private final BulkProductRepository bulkProductRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ── Start Conversation ──────────────────────────────────────────────────────

    @Transactional
    public ConversationResponse startConversation(String wholesalerEmail, StartNegotiationRequest request) {
        User wholesaler = userRepository.findByEmail(wholesalerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", wholesalerEmail));

        BulkProduct bulkProduct = bulkProductRepository.findById(request.getBulkProductId())
                .orElseThrow(() -> new ResourceNotFoundException("BulkProduct", "id", request.getBulkProductId()));

        // Prevent duplicate conversations for same product + wholesaler
        return conversationRepository.findByBulkProductAndWholesaler(bulkProduct, wholesaler)
                .map(ConversationResponse::fromEntity)
                .orElseGet(() -> {
                    Conversation conv = Conversation.builder()
                            .bulkProduct(bulkProduct)
                            .farmer(bulkProduct.getFarmer())
                            .wholesaler(wholesaler)
                            .build();
                    Conversation saved = conversationRepository.save(conv);
                    log.info("Conversation started: {} between farmer {} and wholesaler {}",
                            saved.getId(), bulkProduct.getFarmer().getEmail(), wholesalerEmail);

                    // Send a system message
                    NegotiationMessage systemMsg = NegotiationMessage.builder()
                            .conversation(saved)
                            .sender(wholesaler)
                            .message(wholesaler.getName() + " started a negotiation for " + bulkProduct.getName())
                            .messageType(NegotiationMessage.MessageType.TEXT)
                            .build();
                    messageRepository.save(systemMsg);

                    return ConversationResponse.fromEntity(saved);
                });
    }

    // ── Send Message ────────────────────────────────────────────────────────────

    @Transactional
    public NegotiationMessageResponse sendMessage(String senderEmail, SendNegotiationMessageRequest request) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", senderEmail));

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", request.getConversationId()));

        // Verify sender is part of conversation
        validateConversationAccess(sender, conversation, false);

        NegotiationMessage.MessageType msgType = NegotiationMessage.MessageType.TEXT;
        if (request.getMessageType() != null) {
            try {
                msgType = NegotiationMessage.MessageType.valueOf(request.getMessageType());
            } catch (IllegalArgumentException e) {
                msgType = NegotiationMessage.MessageType.TEXT;
            }
        }

        NegotiationMessage msg = NegotiationMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .message(request.getMessage())
                .messageType(msgType)
                .build();

        NegotiationMessage saved = messageRepository.save(msg);
        NegotiationMessageResponse response = NegotiationMessageResponse.fromEntity(saved);

        // Broadcast via WebSocket
        messagingTemplate.convertAndSend("/topic/negotiation/" + conversation.getId(), response);

        return response;
    }

    // ── Get Messages ────────────────────────────────────────────────────────────

    public List<NegotiationMessageResponse> getMessages(String userEmail, Long conversationId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        // Admin can view (read-only), farmer/wholesaler must be participants
        validateConversationAccess(user, conversation, true);

        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation)
                .stream()
                .map(NegotiationMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ── Get Conversations ───────────────────────────────────────────────────────

    public List<ConversationResponse> getConversations(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        List<Conversation> conversations;

        if (user.getRole() == User.Role.ROLE_ADMIN) {
            conversations = conversationRepository.findAllByOrderByCreatedAtDesc();
        } else if (user.getRole() == User.Role.ROLE_FARMER) {
            conversations = conversationRepository.findByFarmerOrderByCreatedAtDesc(user);
        } else if (user.getRole() == User.Role.ROLE_WHOLESALER) {
            conversations = conversationRepository.findByWholesalerOrderByCreatedAtDesc(user);
        } else {
            throw new RuntimeException("You do not have access to negotiations");
        }

        return conversations.stream()
                .map(ConversationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ── Create Deal Offer ───────────────────────────────────────────────────────

    @Transactional
    public DealOfferResponse createDealOffer(String senderEmail, CreateDealOfferRequest request) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", senderEmail));

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", request.getConversationId()));

        validateConversationAccess(sender, conversation, false);

        BigDecimal totalPrice = request.getPricePerUnit().multiply(BigDecimal.valueOf(request.getQuantity()));

        DealOffer deal = DealOffer.builder()
                .conversation(conversation)
                .createdBy(sender)
                .pricePerUnit(request.getPricePerUnit())
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .build();

        DealOffer saved = dealOfferRepository.save(deal);
        log.info("Deal offer created: {} for conversation: {}", saved.getId(), conversation.getId());

        // Also create a message about the deal
        NegotiationMessage dealMsg = NegotiationMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .message("Deal Offer: ₹" + request.getPricePerUnit() + "/unit × " +
                        request.getQuantity() + " units = ₹" + totalPrice + " total")
                .messageType(NegotiationMessage.MessageType.PRICE_OFFER)
                .build();
        messageRepository.save(dealMsg);

        // Broadcast deal via WebSocket
        DealOfferResponse response = DealOfferResponse.fromEntity(saved);
        messagingTemplate.convertAndSend("/topic/negotiation/" + conversation.getId(),
                NegotiationMessageResponse.fromEntity(dealMsg));

        return response;
    }

    // ── Respond to Deal ─────────────────────────────────────────────────────────

    @Transactional
    public DealOfferResponse respondToDeal(String userEmail, RespondDealRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        DealOffer deal = dealOfferRepository.findById(request.getDealOfferId())
                .orElseThrow(() -> new ResourceNotFoundException("DealOffer", "id", request.getDealOfferId()));

        Conversation conversation = deal.getConversation();
        validateConversationAccess(user, conversation, false);

        // Prevent the offer creator from accepting their own offer
        if ("ACCEPT".equalsIgnoreCase(request.getAction()) && deal.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("You cannot accept your own offer. Wait for the other party to respond.");
        }

        if ("ACCEPT".equalsIgnoreCase(request.getAction())) {
            deal.setStatus(DealOffer.DealStatus.ACCEPTED);
            conversation.setStatus(Conversation.ConversationStatus.AGREED);
            conversationRepository.save(conversation);

            // System message
            NegotiationMessage acceptMsg = NegotiationMessage.builder()
                    .conversation(conversation)
                    .sender(user)
                    .message("Deal accepted! ₹" + deal.getPricePerUnit() + "/unit × " +
                            deal.getQuantity() + " = ₹" + deal.getTotalPrice())
                    .messageType(NegotiationMessage.MessageType.DEAL_ACCEPTED)
                    .build();
            messageRepository.save(acceptMsg);

            messagingTemplate.convertAndSend("/topic/negotiation/" + conversation.getId(),
                    NegotiationMessageResponse.fromEntity(acceptMsg));

        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            deal.setStatus(DealOffer.DealStatus.REJECTED);

            NegotiationMessage rejectMsg = NegotiationMessage.builder()
                    .conversation(conversation)
                    .sender(user)
                    .message("Deal offer rejected.")
                    .messageType(NegotiationMessage.MessageType.DEAL_REJECTED)
                    .build();
            messageRepository.save(rejectMsg);

            messagingTemplate.convertAndSend("/topic/negotiation/" + conversation.getId(),
                    NegotiationMessageResponse.fromEntity(rejectMsg));
        } else {
            throw new RuntimeException("Invalid action. Use ACCEPT or REJECT");
        }

        DealOffer saved = dealOfferRepository.save(deal);
        return DealOfferResponse.fromEntity(saved);
    }

    // ── Get Deals for a Conversation ────────────────────────────────────────────

    public List<DealOfferResponse> getDeals(String userEmail, Long conversationId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        validateConversationAccess(user, conversation, true);

        return dealOfferRepository.findByConversationOrderByCreatedAtDesc(conversation)
                .stream()
                .map(DealOfferResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ── Helper ──────────────────────────────────────────────────────────────────

    private void validateConversationAccess(User user, Conversation conversation, boolean allowAdmin) {
        boolean isFarmer = conversation.getFarmer().getId().equals(user.getId());
        boolean isWholesaler = conversation.getWholesaler().getId().equals(user.getId());
        boolean isAdmin = allowAdmin && user.getRole() == User.Role.ROLE_ADMIN;

        if (!isFarmer && !isWholesaler && !isAdmin) {
            throw new RuntimeException("You do not have access to this conversation");
        }
    }

    // ── Mark Messages as Read ───────────────────────────────────────────────────

    @Transactional
    public void markMessagesAsRead(String userEmail, Long conversationId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        validateConversationAccess(user, conversation, false);

        // Mark all messages not sent by this user as SEEN
        List<NegotiationMessage> unreadMessages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation)
                .stream()
                .filter(msg -> !msg.getSender().getId().equals(user.getId()))
                .filter(msg -> msg.getStatus() != NegotiationMessage.MessageStatus.SEEN)
                .collect(Collectors.toList());

        for (NegotiationMessage msg : unreadMessages) {
            msg.setStatus(NegotiationMessage.MessageStatus.SEEN);
        }

        if (!unreadMessages.isEmpty()) {
            messageRepository.saveAll(unreadMessages);
            log.info("Marked {} messages as read for user {} in conversation {}",
                    unreadMessages.size(), userEmail, conversationId);

            // Broadcast status update via WebSocket
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("type", "READ_RECEIPT");
            statusUpdate.put("conversationId", conversationId);
            statusUpdate.put("readByUserId", user.getId());
            messagingTemplate.convertAndSend("/topic/negotiation/" + conversationId, statusUpdate);
        }
    }
}
