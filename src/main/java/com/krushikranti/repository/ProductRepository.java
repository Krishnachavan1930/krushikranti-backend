package com.krushikranti.repository;

import com.krushikranti.model.Product;
import com.krushikranti.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
        Page<Product> findByFarmer(User farmer, Pageable pageable);

        Page<Product> findByFarmerId(Long farmerId, Pageable pageable);

        Page<Product> findByCategory(String category, Pageable pageable);

        Page<Product> findByStatus(Product.ProductStatus status, Pageable pageable);

        @Query("SELECT p FROM Product p WHERE " +
                        "(:category IS NULL OR p.category = :category) AND " +
                        "(:status IS NULL OR p.status = :status) AND " +
                        "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<Product> findWithFilters(
                        @Param("category") String category,
                        @Param("status") Product.ProductStatus status,
                        @Param("search") String search,
                        Pageable pageable);
}
