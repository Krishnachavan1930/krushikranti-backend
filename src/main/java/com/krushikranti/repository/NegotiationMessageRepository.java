package com.krushikranti.repository;

import com.krushikranti.model.Conversation;
import com.krushikranti.model.NegotiationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NegotiationMessageRepository extends JpaRepository<NegotiationMessage, Long> {

    List<NegotiationMessage> findByConversationOrderByCreatedAtAsc(Conversation conversation);
}
