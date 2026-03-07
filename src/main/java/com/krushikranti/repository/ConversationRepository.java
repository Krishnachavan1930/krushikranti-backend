package com.krushikranti.repository;

import com.krushikranti.model.BulkProduct;
import com.krushikranti.model.Conversation;
import com.krushikranti.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByFarmerOrderByCreatedAtDesc(User farmer);

    List<Conversation> findByWholesalerOrderByCreatedAtDesc(User wholesaler);

    Optional<Conversation> findByBulkProductAndWholesaler(BulkProduct bulkProduct, User wholesaler);

    List<Conversation> findAllByOrderByCreatedAtDesc();
}
