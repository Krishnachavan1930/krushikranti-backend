package com.krushikranti.repository;

import com.krushikranti.model.BulkOrder;
import com.krushikranti.model.DealOffer;
import com.krushikranti.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulkOrderRepository extends JpaRepository<BulkOrder, Long> {

    Optional<BulkOrder> findByDealOffer(DealOffer dealOffer);

    Optional<BulkOrder> findByRazorpayOrderId(String razorpayOrderId);

    List<BulkOrder> findByFarmerOrderByCreatedAtDesc(User farmer);

    List<BulkOrder> findByWholesalerOrderByCreatedAtDesc(User wholesaler);

    List<BulkOrder> findAllByOrderByCreatedAtDesc();
}
