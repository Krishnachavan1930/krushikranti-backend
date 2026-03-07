package com.krushikranti.repository;

import com.krushikranti.model.CartItem;
import com.krushikranti.model.Product;
import com.krushikranti.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUser(User user);

    Optional<CartItem> findByUserAndProduct(User user, Product product);

    void deleteByUser(User user);

    void deleteByUserAndProduct(User user, Product product);

    long countByUser(User user);
}
