package com.krushikranti.service;

import com.krushikranti.dto.request.OrderRequest;
import com.krushikranti.dto.response.OrderResponse;
import com.krushikranti.dto.response.ShipmentResponse;
import com.krushikranti.dto.response.TrackingResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.Order;
import com.krushikranti.model.Product;
import com.krushikranti.model.User;
import com.krushikranti.repository.OrderRepository;
import com.krushikranti.repository.ProductRepository;
import com.krushikranti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

        private final OrderRepository orderRepository;
        private final ProductRepository productRepository;
        private final UserRepository userRepository;
        private final ShiprocketService shiprocketService;
        private final NotificationService notificationService;

        @Value("${app.commission.admin-percentage:0.10}")
        private BigDecimal adminCommissionPercentage;

        @Value("${app.commission.farmer-percentage:0.90}")
        private BigDecimal farmerAmountPercentage;

        @Transactional
        public OrderResponse createOrder(String userEmail, OrderRequest request) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

                Product product = productRepository.findById(request.getProductId())
                                .orElseThrow(() -> new ResourceNotFoundException("Product", "id",
                                                request.getProductId()));

                if (product.getQuantity() < request.getQuantity()) {
                        throw new RuntimeException("Insufficient product stock");
                }

                BigDecimal totalPrice = product.getRetailPrice().multiply(new BigDecimal(request.getQuantity()));

                // Calculate commission
                BigDecimal adminCommission = totalPrice.multiply(adminCommissionPercentage)
                                .setScale(2, RoundingMode.HALF_UP);
                BigDecimal farmerAmount = totalPrice.multiply(farmerAmountPercentage)
                                .setScale(2, RoundingMode.HALF_UP);

                Order order = Order.builder()
                                .user(user)
                                .product(product)
                                .quantity(request.getQuantity())
                                .totalPrice(totalPrice)
                                .status(Order.OrderStatus.PENDING)
                                .adminCommission(adminCommission)
                                .farmerAmount(farmerAmount)
                                .deliveryStatus(Order.DeliveryStatus.PENDING)
                                .shippingAddress(request.getShippingAddress())
                                .shippingCity(request.getShippingCity())
                                .shippingState(request.getShippingState())
                                .shippingPincode(request.getShippingPincode())
                                .customerPhone(request.getCustomerPhone())
                                .build();

                // Decrement stock in product
                product.setQuantity(product.getQuantity() - request.getQuantity());
                productRepository.save(product);

                Order savedOrder = orderRepository.save(order);

                // Send notification to farmer
                try {
                        notificationService.notifyNewOrder(savedOrder);
                } catch (Exception e) {
                        log.error("Failed to send order notification", e);
                }

                return OrderResponse.fromEntity(savedOrder);
        }

        public List<OrderResponse> getUserOrders(String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
                return orderRepository.findByUser(user).stream()
                                .map(OrderResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        public Page<OrderResponse> getUserOrdersPaginated(String userEmail, int page, int size, Order.OrderStatus status) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                
                Page<Order> orders;
                if (status != null) {
                        orders = orderRepository.findByUserAndStatus(user, status, pageable);
                } else {
                        orders = orderRepository.findByUser(user, pageable);
                }
                return orders.map(OrderResponse::fromEntity);
        }

        public List<OrderResponse> getFarmerOrders(String farmerEmail) {
                User farmer = userRepository.findByEmail(farmerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", farmerEmail));
                return orderRepository.findByProductFarmer(farmer).stream()
                                .map(OrderResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        public Page<OrderResponse> getFarmerOrdersPaginated(String farmerEmail, int page, int size, Order.OrderStatus status) {
                User farmer = userRepository.findByEmail(farmerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", farmerEmail));
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                
                Page<Order> orders;
                if (status != null) {
                        orders = orderRepository.findByProductFarmerAndStatus(farmer, status, pageable);
                } else {
                        orders = orderRepository.findByProductFarmer(farmer, pageable);
                }
                return orders.map(OrderResponse::fromEntity);
        }

        public OrderResponse getOrderById(Long orderId) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
                return OrderResponse.fromEntity(order);
        }

        @Transactional
        public OrderResponse updateOrderStatus(Long orderId, Order.OrderStatus status) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

                String oldStatus = order.getStatus().name();
                order.setStatus(status);
                
                Order savedOrder = orderRepository.save(order);

                // Send notification
                try {
                        notificationService.notifyOrderStatusChange(savedOrder, oldStatus, status.name());
                } catch (Exception e) {
                        log.error("Failed to send order status notification", e);
                }

                return OrderResponse.fromEntity(savedOrder);
        }

        /**
         * Process payment and create shipment
         */
        @Transactional
        public OrderResponse processPaymentAndCreateShipment(Long orderId) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

                if (order.getStatus() != Order.OrderStatus.CONFIRMED) {
                        throw new RuntimeException("Order must be in CONFIRMED status to create shipment");
                }

                // Create shipment with Shiprocket
                try {
                        ShipmentResponse shipmentResponse = shiprocketService.createShipment(order);
                        
                        if (shipmentResponse.isSuccess()) {
                                order.setShipmentId(shipmentResponse.getShipmentId());
                                order.setAwbCode(shipmentResponse.getAwbCode());
                                order.setCourierName(shipmentResponse.getCourierName());
                                order.setDeliveryStatus(Order.DeliveryStatus.PICKUP_SCHEDULED);
                                order.setTrackingStatus("Shipment Created");
                                
                                if (shipmentResponse.getAwbCode() != null) {
                                        order.setStatus(Order.OrderStatus.SHIPPED);
                                }
                                
                                orderRepository.save(order);
                                
                                // Send notification
                                notificationService.notifyShipmentUpdate(order, "Shipment Created");
                        }
                } catch (Exception e) {
                        log.error("Failed to create shipment for order: {}", orderId, e);
                }

                return OrderResponse.fromEntity(order);
        }

        /**
         * Get tracking information for an order
         */
        public TrackingResponse getOrderTracking(Long orderId) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

                TrackingResponse response = new TrackingResponse();
                response.setOrderId(orderId);
                response.setOrderStatus(order.getStatus().name());
                response.setDeliveryStatus(order.getDeliveryStatus() != null ? order.getDeliveryStatus().name() : null);
                response.setCourierName(order.getCourierName());
                response.setAwbCode(order.getAwbCode());

                // If we have AWB code, fetch real-time tracking from Shiprocket
                if (order.getAwbCode() != null && !order.getAwbCode().isEmpty()) {
                        try {
                                TrackingResponse shiprocketTracking = shiprocketService.getTrackingInfo(order.getAwbCode());
                                if (shiprocketTracking.isSuccess()) {
                                        response.setCurrentStatus(shiprocketTracking.getCurrentStatus());
                                        response.setCurrentLocation(shiprocketTracking.getCurrentLocation());
                                        response.setEstimatedDelivery(shiprocketTracking.getEstimatedDelivery());
                                        response.setTrackingActivities(shiprocketTracking.getTrackingActivities());
                                        response.setTrackingUrl(shiprocketTracking.getTrackingUrl());
                                        response.setSuccess(true);
                                }
                        } catch (Exception e) {
                                log.error("Failed to fetch tracking info for order: {}", orderId, e);
                                response.setSuccess(false);
                                response.setMessage("Unable to fetch real-time tracking");
                        }
                } else {
                        response.setSuccess(true);
                        response.setCurrentStatus(order.getTrackingStatus());
                }

                return response;
        }

        /**
         * Get all orders for admin
         */
        public Page<OrderResponse> getAllOrders(int page, int size, Order.OrderStatus status) {
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                
                Page<Order> orders;
                if (status != null) {
                        orders = orderRepository.findByStatus(status, pageable);
                } else {
                        orders = orderRepository.findAll(pageable);
                }
                return orders.map(OrderResponse::fromEntity);
        }

        /**
         * Get admin dashboard stats
         */
        public AdminOrderStats getAdminStats() {
                List<Order> allOrders = orderRepository.findAll();
                
                long totalOrders = allOrders.size();
                BigDecimal totalRevenue = allOrders.stream()
                                .filter(o -> o.getStatus() != Order.OrderStatus.CANCELLED)
                                .map(Order::getTotalPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalCommission = allOrders.stream()
                                .filter(o -> o.getStatus() != Order.OrderStatus.CANCELLED)
                                .map(o -> o.getAdminCommission() != null ? o.getAdminCommission() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                long pendingOrders = allOrders.stream()
                                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING || 
                                             o.getStatus() == Order.OrderStatus.CONFIRMED)
                                .count();
                long shippedOrders = allOrders.stream()
                                .filter(o -> o.getStatus() == Order.OrderStatus.SHIPPED || 
                                             o.getStatus() == Order.OrderStatus.PROCESSING)
                                .count();
                long deliveredOrders = allOrders.stream()
                                .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED)
                                .count();

                return AdminOrderStats.builder()
                                .totalOrders(totalOrders)
                                .totalRevenue(totalRevenue)
                                .totalCommission(totalCommission)
                                .pendingOrders(pendingOrders)
                                .shippedOrders(shippedOrders)
                                .deliveredOrders(deliveredOrders)
                                .build();
        }

        /**
         * Get farmer earnings stats
         */
        public FarmerOrderStats getFarmerStats(String farmerEmail) {
                User farmer = userRepository.findByEmail(farmerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", farmerEmail));
                
                List<Order> farmerOrders = orderRepository.findByProductFarmer(farmer);
                
                long totalOrders = farmerOrders.size();
                BigDecimal totalEarnings = farmerOrders.stream()
                                .filter(o -> o.getStatus() != Order.OrderStatus.CANCELLED)
                                .map(o -> o.getFarmerAmount() != null ? o.getFarmerAmount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                long pendingOrders = farmerOrders.stream()
                                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING || 
                                             o.getStatus() == Order.OrderStatus.CONFIRMED ||
                                             o.getStatus() == Order.OrderStatus.PROCESSING)
                                .count();
                long completedOrders = farmerOrders.stream()
                                .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED)
                                .count();

                return FarmerOrderStats.builder()
                                .totalOrders(totalOrders)
                                .totalEarnings(totalEarnings)
                                .pendingOrders(pendingOrders)
                                .completedOrders(completedOrders)
                                .build();
        }

        @lombok.Data
        @lombok.Builder
        @lombok.AllArgsConstructor
        @lombok.NoArgsConstructor
        public static class AdminOrderStats {
                private long totalOrders;
                private BigDecimal totalRevenue;
                private BigDecimal totalCommission;
                private long pendingOrders;
                private long shippedOrders;
                private long deliveredOrders;
        }

        @lombok.Data
        @lombok.Builder
        @lombok.AllArgsConstructor
        @lombok.NoArgsConstructor
        public static class FarmerOrderStats {
                private long totalOrders;
                private BigDecimal totalEarnings;
                private long pendingOrders;
                private long completedOrders;
        }
}
