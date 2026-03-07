package com.krushikranti.dto.request;

import com.krushikranti.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryStatusUpdateRequest {

    @NotNull(message = "Delivery status is required")
    private Order.DeliveryStatus deliveryStatus;
}
