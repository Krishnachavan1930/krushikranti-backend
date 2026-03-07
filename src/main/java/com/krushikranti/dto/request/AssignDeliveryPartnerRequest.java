package com.krushikranti.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssignDeliveryPartnerRequest {

    @NotNull(message = "Delivery partner ID is required")
    private Long deliveryPartnerId;

    private String notes;
}
