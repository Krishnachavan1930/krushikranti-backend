package com.krushikranti.repository;

import com.krushikranti.model.BulkProduct;
import com.krushikranti.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BulkProductRepository extends JpaRepository<BulkProduct, Long> {

    List<BulkProduct> findByFarmerOrderByCreatedAtDesc(User farmer);

    List<BulkProduct> findByStatusOrderByCreatedAtDesc(BulkProduct.BulkProductStatus status);
}
