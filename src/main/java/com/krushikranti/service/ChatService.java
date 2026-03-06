package com.krushikranti.service;

import com.krushikranti.dto.request.SendMessageRequest;
import com.krushikranti.dto.response.ChatMessageResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.ChatMessage;
import com.krushikranti.model.User;
import com.krushikranti.repository.ChatMessageRepository;
import com.krushikranti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessageResponse sendMessage(String senderEmail, SendMessageRequest request) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", senderEmail));

        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getReceiverId()));

        ChatMessage chatMessage = ChatMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .message(request.getMessage())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        return ChatMessageResponse.fromEntity(savedMessage);
    }

    public List<ChatMessageResponse> getConversation(String userEmail, Long otherUserId) {
        User user1 = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        User user2 = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", otherUserId));

        Sort sort = Sort.by(Sort.Direction.ASC, "timestamp");
        List<ChatMessage> conversation = chatMessageRepository.findConversation(user1, user2, sort);

        return conversation.stream()
                .map(ChatMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
