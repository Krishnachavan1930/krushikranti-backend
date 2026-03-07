package com.krushikranti.dto.response;

import com.krushikranti.model.BulkOrder;
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
public class BulkOrderResponse {

    private Long id;
    private Long dealOfferId;
    private Long farmerId;
    private String farmerName;
    private Long wholesalerId;
    private String wholesalerName;
    private String productName;
    private Integer quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal totalAmount;
    private BigDecimal platformFee;
    private BigDecimal farmerPayout;
    private String paymentStatus;
    private String orderStatus;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String shipmentId;
    private String awbCode;
    private String courierName;
    private String trackingUrl;
    private String deliveryStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BulkOrderResponse fromEntity(BulkOrder order) {
        return BulkOrderResponse.builder()
                .id(order.getId())
                .dealOfferId(order.getDealOffer().getId())
                .farmerId(order.getFarmer().getId())
                .farmerName(order.getFarmer().getName())
                .wholesalerId(order.getWholesaler().getId())
                .wholesalerName(order.getWholesaler().getName())
                .productName(order.getBulkProduct().getName())
                .quantity(order.getDealOffer().getQuantity())
                .pricePerUnit(order.getDealOffer().getPricePerUnit())
                .totalAmount(order.getTotalAmount())
                .platformFee(order.getPlatformFee())
                .farmerPayout(order.getFarmerPayout())
                .paymentStatus(order.getPaymentStatus().name())
                .orderStatus(order.getOrderStatus().name())
                .razorpayOrderId(order.getRazorpayOrderId())
                .razorpayPaymentId(order.getRazorpayPaymentId())
                .shipmentId(order.getShipmentId())
                .awbCode(order.getAwbCode())
                .courierName(order.getCourierName())
                .trackingUrl(order.getTrackingUrl())
                .deliveryStatus(order.getDeliveryStatus().name())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
