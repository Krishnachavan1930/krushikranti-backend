package com.krushikranti.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentResponse {
    private boolean success;
    private String message;
    private String shipmentId;
    private String shiprocketOrderId;
    private String awbCode;
    private String courierName;
    private String trackingUrl;
}
