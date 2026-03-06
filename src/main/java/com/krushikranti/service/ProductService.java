package com.krushikranti.service;

import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.Product;
import com.krushikranti.model.User;
import com.krushikranti.repository.ProductRepository;
import com.krushikranti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public Page<Product> getAllProducts(String category, String search, Pageable pageable) {
        return productRepository.findWithFilters(category, Product.ProductStatus.ACTIVE, search, pageable);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    public Page<Product> getProductsByFarmer(Long farmerId, Pageable pageable) {
        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", farmerId));
        return productRepository.findByFarmer(farmer, pageable);
    }

    @Transactional
    public Product createProduct(Product product, Long farmerId) {
        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", farmerId));
        product.setFarmer(farmer);
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, Product updatedProduct) {
        Product existing = getProductById(id);
        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setStock(updatedProduct.getStock());
        existing.setUnit(updatedProduct.getUnit());
        existing.setCategory(updatedProduct.getCategory());
        existing.setImageUrl(updatedProduct.getImageUrl());
        existing.setStatus(updatedProduct.getStatus());
        return productRepository.save(existing);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }
}
