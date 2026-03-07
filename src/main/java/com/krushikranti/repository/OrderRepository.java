package com.krushikranti.repository;

import com.krushikranti.model.Order;
import com.krushikranti.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);

    Page<Order> findByUser(User user, Pageable pageable);

    Page<Order> findByUserAndStatus(User user, Order.OrderStatus status, Pageable pageable);

    // Find all orders where the product belongs to the given farmer
    List<Order> findByProductFarmer(User farmer);

    Page<Order> findByProductFarmer(User farmer, Pageable pageable);

    Page<Order> findByProductFarmerAndStatus(User farmer, Order.OrderStatus status, Pageable pageable);

    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    @Query("SELECT o FROM Order o WHERE o.awbCode = :awbCode")
    Optional<Order> findByAwbCode(@Param("awbCode") String awbCode);

    @Query("SELECT o FROM Order o WHERE o.shipmentId = :shipmentId")
    Optional<Order> findByShipmentId(@Param("shipmentId") String shipmentId);

    // Count orders by status
    long countByStatus(Order.OrderStatus status);

    // Find orders by delivery status
    List<Order> findByDeliveryStatus(Order.DeliveryStatus deliveryStatus);

    // Delivery partner queries
    Page<Order> findByDeliveryPartner(User deliveryPartner, Pageable pageable);

    Page<Order> findByDeliveryPartnerAndDeliveryStatus(User deliveryPartner, Order.DeliveryStatus deliveryStatus,
            Pageable pageable);

    // Find order by order number
    Optional<Order> findByOrderNumber(String orderNumber);
}
