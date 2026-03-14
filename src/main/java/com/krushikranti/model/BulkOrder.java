package com.krushikranti.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_offer_id", nullable = false, unique = true)
    private DealOffer dealOffer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wholesaler_id", nullable = false)
    private User wholesaler;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bulk_product_id", nullable = false)
    private BulkProduct bulkProduct;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "platform_fee", precision = 10, scale = 2)
    private BigDecimal platformFee; // 5% to KrushiKranti

    @Column(name = "farmer_payout", precision = 12, scale = 2)
    private BigDecimal farmerPayout; // 95% to Farmer

    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "invoice_path", length = 500)
    private String invoicePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.AWAITING_PAYMENT;

    // Shiprocket fields
    @Column(name = "shipment_id")
    private String shipmentId;

    @Column(name = "awb_code", length = 50)
    private String awbCode;

    @Column(name = "courier_name", length = 100)
    private String courierName;

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 30)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.NOT_SHIPPED;

    // Shipping Address Fields
    @Column(name = "shipping_name", length = 100)
    private String shippingName;

    @Column(name = "shipping_phone", length = 20)
    private String shippingPhone;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    @Column(name = "shipping_state", length = 100)
    private String shippingState;

    @Column(name = "shipping_pincode", length = 10)
    private String shippingPincode;

    // Estimated Delivery
    private LocalDateTime estimatedDelivery;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED
    }

    public enum OrderStatus {
        AWAITING_PAYMENT, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }

    public enum DeliveryStatus {
        NOT_SHIPPED, PICKUP_SCHEDULED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, RETURNED
    }
}
