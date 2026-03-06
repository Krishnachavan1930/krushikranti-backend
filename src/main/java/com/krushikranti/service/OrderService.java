package com.krushikranti.service;

import com.krushikranti.dto.request.OrderRequest;
import com.krushikranti.dto.response.OrderResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.Order;
import com.krushikranti.model.Product;
import com.krushikranti.model.User;
import com.krushikranti.repository.OrderRepository;
import com.krushikranti.repository.ProductRepository;
import com.krushikranti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(String userEmail, OrderRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (product.getStock() < request.getQuantity()) {
            throw new RuntimeException("Insufficient product stock");
        }

        BigDecimal totalPrice = product.getPrice().multiply(new BigDecimal(request.getQuantity()));

        Order order = Order.builder()
                .user(user)
                .product(product)
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .status(Order.OrderStatus.PLACED)
                .build();

        // Decrement stock in product
        product.setStock(product.getStock() - request.getQuantity());
        productRepository.save(product);

        Order savedOrder = orderRepository.save(order);
        return OrderResponse.fromEntity(savedOrder);
    }

    public List<OrderResponse> getUserOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        return orderRepository.findByUser(user).stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getFarmerOrders(String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", farmerEmail));
        return orderRepository.findByProductFarmer(farmer).stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        order.setStatus(status);
        return OrderResponse.fromEntity(orderRepository.save(order));
    }
}
