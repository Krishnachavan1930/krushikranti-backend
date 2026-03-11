package com.krushikranti.repository;

import com.krushikranti.model.BulkOrder;
import com.krushikranti.model.DealOffer;
import com.krushikranti.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BulkOrderRepository extends JpaRepository<BulkOrder, Long> {

    Optional<BulkOrder> findByDealOffer(DealOffer dealOffer);

    Optional<BulkOrder> findByRazorpayOrderId(String razorpayOrderId);

    List<BulkOrder> findByFarmerOrderByCreatedAtDesc(User farmer);

    List<BulkOrder> findByWholesalerOrderByCreatedAtDesc(User wholesaler);

    List<BulkOrder> findAllByOrderByCreatedAtDesc();

    // ── Farmer Analytics ────────────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(bo.farmerPayout), 0) FROM BulkOrder bo " +
           "WHERE bo.farmer = :farmer AND bo.paymentStatus = :status")
    BigDecimal sumFarmerPayoutByStatus(@Param("farmer") User farmer,
                                       @Param("status") BulkOrder.PaymentStatus status);

    @Query("SELECT COUNT(bo) FROM BulkOrder bo WHERE bo.farmer = :farmer")
    Long countTotalByFarmer(@Param("farmer") User farmer);

    @Query("SELECT COUNT(bo) FROM BulkOrder bo WHERE bo.farmer = :farmer AND bo.orderStatus = :status")
    Long countByFarmerAndOrderStatus(@Param("farmer") User farmer,
                                     @Param("status") BulkOrder.OrderStatus status);

    @Query("SELECT COUNT(bo) FROM BulkOrder bo WHERE bo.farmer = :farmer " +
           "AND bo.orderStatus NOT IN :excludedStatuses")
    Long countByFarmerExcludingStatuses(@Param("farmer") User farmer,
                                        @Param("excludedStatuses") List<BulkOrder.OrderStatus> excludedStatuses);

    @Query("SELECT bo.bulkProduct.name FROM BulkOrder bo " +
           "WHERE bo.farmer = :farmer AND bo.paymentStatus = :paymentStatus " +
           "GROUP BY bo.bulkProduct.id, bo.bulkProduct.name " +
           "ORDER BY COUNT(bo) DESC")
    List<String> findTopProductNamesByFarmer(@Param("farmer") User farmer,
                                              @Param("paymentStatus") BulkOrder.PaymentStatus paymentStatus,
                                              Pageable pageable);
}
