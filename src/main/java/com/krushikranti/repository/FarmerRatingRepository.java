package com.krushikranti.repository;

import com.krushikranti.model.FarmerRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerRatingRepository extends JpaRepository<FarmerRating, Long> {

    List<FarmerRating> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);

    boolean existsByWholesalerIdAndOrderId(Long wholesalerId, Long orderId);

    Optional<FarmerRating> findByWholesalerIdAndOrderId(Long wholesalerId, Long orderId);

    @Query("SELECT AVG(r.rating) FROM FarmerRating r WHERE r.farmer.id = :farmerId")
    Double findAverageRatingByFarmerId(@Param("farmerId") Long farmerId);

    @Query("SELECT COUNT(r) FROM FarmerRating r WHERE r.farmer.id = :farmerId")
    Long countByFarmerId(@Param("farmerId") Long farmerId);
}
