package com.krushikranti.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentOrderResponse {

    private String id; // The razorpay order ID generated
    private String currency;
    private Integer amount;
    private String status;
}
