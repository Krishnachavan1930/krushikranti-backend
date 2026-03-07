package com.krushikranti.dto.request;

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
public class OrderNotificationEvent {
    private Long orderId;
    private String productName;
    private String customerName;
    private Integer quantity;
    private BigDecimal totalAmount;
    private LocalDateTime timestamp;
    private String orderStatus;
    private String deliveryStatus;
}
