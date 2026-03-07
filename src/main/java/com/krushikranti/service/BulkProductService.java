package com.krushikranti.service;

import com.krushikranti.dto.request.CreateBulkProductRequest;
import com.krushikranti.dto.response.BulkProductResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.BulkProduct;
import com.krushikranti.model.User;
import com.krushikranti.repository.BulkProductRepository;
import com.krushikranti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkProductService {

        private final BulkProductRepository bulkProductRepository;
        private final UserRepository userRepository;

        @Transactional
        public BulkProductResponse createBulkProduct(String farmerEmail, CreateBulkProductRequest request) {
                User farmer = userRepository.findByEmail(farmerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", farmerEmail));

                BulkProduct bp = BulkProduct.builder()
                                .farmer(farmer)
                                .name(request.getName())
                                .description(request.getDescription())
                                .quantity(request.getQuantity())
                                .minimumPrice(request.getMinimumPrice())
                                .location(request.getLocation())
                                .imageUrl(request.getImageUrl())
                                .build();

                BulkProduct saved = bulkProductRepository.save(bp);
                log.info("Bulk product created: {} by farmer: {}", saved.getId(), farmer.getEmail());
                return BulkProductResponse.fromEntity(saved);
        }

        public List<BulkProductResponse> getAllActiveBulkProducts() {
                return bulkProductRepository.findByStatusOrderByCreatedAtDesc(BulkProduct.BulkProductStatus.ACTIVE)
                                .stream()
                                .map(BulkProductResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        public List<BulkProductResponse> getMyBulkProducts(String farmerEmail) {
                User farmer = userRepository.findByEmail(farmerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", farmerEmail));

                return bulkProductRepository.findByFarmerOrderByCreatedAtDesc(farmer)
                                .stream()
                                .map(BulkProductResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        @Transactional
        public BulkProductResponse updateBulkProduct(String farmerEmail, Long productId,
                        CreateBulkProductRequest request) {
                User farmer = userRepository.findByEmail(farmerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", farmerEmail));

                BulkProduct bp = bulkProductRepository.findById(productId)
                                .orElseThrow(() -> new ResourceNotFoundException("BulkProduct", "id", productId));

                if (!bp.getFarmer().getId().equals(farmer.getId())) {
                        throw new RuntimeException("You can only update your own bulk products");
                }

                bp.setName(request.getName());
                bp.setDescription(request.getDescription());
                bp.setQuantity(request.getQuantity());
                bp.setMinimumPrice(request.getMinimumPrice());
                bp.setLocation(request.getLocation());
                bp.setImageUrl(request.getImageUrl());

                BulkProduct saved = bulkProductRepository.save(bp);
                return BulkProductResponse.fromEntity(saved);
        }

        @Transactional
        public void deleteBulkProduct(String farmerEmail, Long productId) {
                User farmer = userRepository.findByEmail(farmerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", farmerEmail));

                BulkProduct bp = bulkProductRepository.findById(productId)
                                .orElseThrow(() -> new ResourceNotFoundException("BulkProduct", "id", productId));

                if (!bp.getFarmer().getId().equals(farmer.getId())) {
                        throw new RuntimeException("You can only delete your own bulk products");
                }

                bulkProductRepository.delete(bp);
                log.info("Bulk product deleted: {} by farmer: {}", productId, farmerEmail);
        }
}
