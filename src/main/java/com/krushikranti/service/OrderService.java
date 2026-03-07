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
                                .orderNumber("KK-"
                                                + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                .user(user)
                                .buyer(user)
                                .product(product)
                                .quantity(request.getQuantity())
                                .totalAmount(totalPrice)
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

        public Page<OrderResponse> getUserOrdersPaginated(String userEmail, int page, int size,
                        Order.OrderStatus status) {
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

        public Page<OrderResponse> getFarmerOrdersPaginated(String farmerEmail, int page, int size,
                        Order.OrderStatus status) {
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
                response.setOrderNumber(order.getOrderNumber());
                response.setOrderStatus(order.getStatus().name());
                response.setDeliveryStatus(order.getDeliveryStatus() != null ? order.getDeliveryStatus().name() : null);
                response.setCourierName(order.getCourierName());
                response.setAwbCode(order.getAwbCode());
                response.setEstimatedDelivery(
                                order.getEstimatedDelivery() != null ? order.getEstimatedDelivery().toString() : null);

                // Delivery partner info
                response.setDeliveryPartnerName(order.getDeliveryPartnerName());
                response.setDeliveryPartnerPhone(order.getDeliveryPartnerPhone());

                // If we have AWB code, fetch real-time tracking from Shiprocket
                if (order.getAwbCode() != null && !order.getAwbCode().isEmpty()) {
                        try {
                                TrackingResponse shiprocketTracking = shiprocketService
                                                .getTrackingInfo(order.getAwbCode());
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
         * Get tracking information for an order by order number
         */
        public TrackingResponse getOrderTrackingByOrderNumber(String orderNumber) {
                Order order = orderRepository.findByOrderNumber(orderNumber)
                                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
                return getOrderTracking(order.getId());
        }

        /**
         * Cancel an order (only if status is PENDING)
         */
        @Transactional
        public OrderResponse cancelOrder(Long orderId, String userEmail) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

                // Verify ownership or admin
                boolean isAdmin = user.getRole() == User.Role.ROLE_ADMIN;
                if (!isAdmin && !order.getUser().getId().equals(user.getId())) {
                        throw new SecurityException("You can only cancel your own orders");
                }

                // Only allow cancellation of PENDING orders
                if (order.getStatus() != Order.OrderStatus.PENDING) {
                        throw new IllegalStateException("Only pending orders can be cancelled. Current status: " + order.getStatus());
                }

                String oldStatus = order.getStatus().name();
                order.setStatus(Order.OrderStatus.CANCELLED);

                // Restore product stock
                order.getProduct().setQuantity(order.getProduct().getQuantity() + order.getQuantity());

                Order savedOrder = orderRepository.save(order);

                // Send notification
                try {
                        notificationService.notifyOrderStatusChange(savedOrder, oldStatus, "CANCELLED");
                } catch (Exception e) {
                        log.error("Failed to send order cancellation notification", e);
                }

                return OrderResponse.fromEntity(savedOrder);
        }

        /**
         * Assign a delivery partner to an order (Admin only)
         */
        @Transactional
        public OrderResponse assignDeliveryPartner(Long orderId, Long deliveryPartnerId, String notes) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

                User deliveryPartner = userRepository.findById(deliveryPartnerId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", deliveryPartnerId));

                order.setDeliveryPartner(deliveryPartner);
                order.setDeliveryPartnerName(
                                deliveryPartner.getFirstName() + " " +
                                                (deliveryPartner.getLastName() != null ? deliveryPartner.getLastName()
                                                                : ""));
                order.setDeliveryPartnerPhone(deliveryPartner.getPhone());
                if (notes != null && !notes.isEmpty()) {
                        order.setDeliveryNotes(notes);
                }

                Order savedOrder = orderRepository.save(order);

                // Send WebSocket notification to delivery partner
                try {
                        notificationService.createNotification(
                                        deliveryPartner,
                                        "New Delivery Assignment",
                                        String.format("You have been assigned to deliver Order #%s (%s)",
                                                        order.getOrderNumber(),
                                                        order.getProduct().getName()),
                                        com.krushikranti.model.Notification.NotificationType.SYSTEM,
                                        orderId,
                                        "ORDER");
                } catch (Exception e) {
                        log.error("Failed to notify delivery partner for order: {}", orderId, e);
                }

                return OrderResponse.fromEntity(savedOrder);
        }

        /**
         * Update delivery status (by delivery partner or admin)
         */
        @Transactional
        public OrderResponse updateDeliveryStatus(Long orderId, Order.DeliveryStatus newDeliveryStatus) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

                String oldStatus = order.getDeliveryStatus() != null ? order.getDeliveryStatus().name() : "NONE";
                order.setDeliveryStatus(newDeliveryStatus);

                // Auto-update order status based on delivery status
                switch (newDeliveryStatus) {
                        case PICKED_UP, IN_TRANSIT -> {
                                order.setStatus(Order.OrderStatus.SHIPPED);
                                order.setTrackingStatus("In Transit");
                        }
                        case OUT_FOR_DELIVERY -> {
                                order.setStatus(Order.OrderStatus.SHIPPED);
                                order.setTrackingStatus("Out for Delivery");
                        }
                        case DELIVERED -> {
                                order.setStatus(Order.OrderStatus.DELIVERED);
                                order.setTrackingStatus("Delivered");
                        }
                        case CANCELLED, RETURNED -> {
                                order.setTrackingStatus(newDeliveryStatus.name());
                        }
                        default -> {
                        }
                }

                Order savedOrder = orderRepository.save(order);

                // Send WebSocket notification to customer
                try {
                        notificationService.notifyOrderStatusChange(savedOrder, oldStatus,
                                        newDeliveryStatus.name());
                } catch (Exception e) {
                        log.error("Failed to send delivery status notification for order: {}", orderId, e);
                }

                return OrderResponse.fromEntity(savedOrder);
        }

        /**
         * Get orders assigned to a delivery partner
         */
        public Page<OrderResponse> getDeliveryPartnerOrders(String deliveryPartnerEmail, int page, int size,
                        Order.DeliveryStatus deliveryStatus) {
                User deliveryPartner = userRepository.findByEmail(deliveryPartnerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email",
                                                deliveryPartnerEmail));
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

                Page<Order> orders;
                if (deliveryStatus != null) {
                        orders = orderRepository.findByDeliveryPartnerAndDeliveryStatus(deliveryPartner,
                                        deliveryStatus, pageable);
                } else {
                        orders = orderRepository.findByDeliveryPartner(deliveryPartner, pageable);
                }
                return orders.map(OrderResponse::fromEntity);
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
