package com.krushikranti.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentVerificationResponse {

    private boolean verified;
    private Long primaryOrderId;
    private List<Long> orderIds;
}