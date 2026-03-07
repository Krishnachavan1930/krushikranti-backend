package com.krushikranti.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrackingResponse {
    private boolean success;
    private String message;
    private String awbCode;
    private String currentStatus;
    private String currentLocation;
    private String estimatedDelivery;
    private String trackingUrl;
    private String courierName;
    private List<Map<String, Object>> trackingActivities;
    
    // Order details
    private Long orderId;
    private String orderStatus;
    private String deliveryStatus;
}
