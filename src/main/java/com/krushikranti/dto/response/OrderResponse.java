package com.krushikranti.dto.response;

import com.krushikranti.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
        private Long id;
        private String orderNumber;
        private Long userId;
        private String userEmail;
        private String userName;
        private Long productId;
        private String productName;
        private String productImage;
        private Integer quantity;
        private BigDecimal totalAmount;
        private BigDecimal totalPrice;
        private Order.OrderStatus status;
        private LocalDateTime createdAt;

        // Shiprocket tracking fields
        private String shipmentId;
        private String awbCode;
        private String courierName;
        private String trackingStatus;
        private String deliveryStatus;
        private LocalDateTime estimatedDelivery;

        // Commission fields
        private BigDecimal adminCommission;
        private BigDecimal farmerAmount;

        // Farmer details
        private Long farmerId;
        private String farmerName;

        // Shipping address
        private String shippingAddress;
        private String shippingCity;
        private String shippingState;
        private String shippingPincode;
        private String customerPhone;

        // Delivery partner
        private Long deliveryPartnerId;
        private String deliveryPartnerName;
        private String deliveryPartnerPhone;
        private String deliveryNotes;

        public static OrderResponse fromEntity(Order order) {
                OrderResponseBuilder builder = OrderResponse.builder()
                                .id(order.getId())
                                .orderNumber(order.getOrderNumber())
                                .userId(order.getUser().getId())
                                .userEmail(order.getUser().getEmail())
                                .userName(order.getUser().getFirstName() + " " +
                                                (order.getUser().getLastName() != null ? order.getUser().getLastName()
                                                                : ""))
                                .productId(order.getProduct().getId())
                                .productName(order.getProduct().getName())
                                .productImage(order.getProduct().getImageUrl())
                                .quantity(order.getQuantity())
                                .totalAmount(order.getTotalAmount())
                                .totalPrice(order.getTotalPrice())
                                .status(order.getStatus())
                                .createdAt(order.getCreatedAt())
                                // Tracking
                                .shipmentId(order.getShipmentId())
                                .awbCode(order.getAwbCode())
                                .courierName(order.getCourierName())
                                .trackingStatus(order.getTrackingStatus())
                                .deliveryStatus(order.getDeliveryStatus() != null ? order.getDeliveryStatus().name()
                                                : null)
                                .estimatedDelivery(order.getEstimatedDelivery())
                                // Commission
                                .adminCommission(order.getAdminCommission())
                                .farmerAmount(order.getFarmerAmount())
                                // Shipping
                                .shippingAddress(order.getShippingAddress())
                                .shippingCity(order.getShippingCity())
                                .shippingState(order.getShippingState())
                                .shippingPincode(order.getShippingPincode())
                                .customerPhone(order.getCustomerPhone())
                                // Delivery partner
                                .deliveryPartnerName(order.getDeliveryPartnerName())
                                .deliveryPartnerPhone(order.getDeliveryPartnerPhone())
                                .deliveryNotes(order.getDeliveryNotes());

                // Add farmer details
                if (order.getProduct().getFarmer() != null) {
                        builder.farmerId(order.getProduct().getFarmer().getId())
                                        .farmerName(order.getProduct().getFarmer().getFirstName() + " " +
                                                        (order.getProduct().getFarmer().getLastName() != null
                                                                        ? order.getProduct().getFarmer().getLastName()
                                                                        : ""));
                }

                return builder.build();
        }
}
