package com.krushikranti.repository;

import com.krushikranti.model.Order;
import com.krushikranti.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);

    // Find all orders where the product belongs to the given farmer
    List<Order> findByProductFarmer(User farmer);
}
