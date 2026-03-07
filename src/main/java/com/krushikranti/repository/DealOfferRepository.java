package com.krushikranti.repository;

import com.krushikranti.model.Conversation;
import com.krushikranti.model.DealOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DealOfferRepository extends JpaRepository<DealOffer, Long> {

    List<DealOffer> findByConversationOrderByCreatedAtDesc(Conversation conversation);

    List<DealOffer> findByConversationAndStatus(Conversation conversation, DealOffer.DealStatus status);
}
