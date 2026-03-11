package com.krushikranti.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutPaymentInitiateRequest {

    @Valid
    @NotEmpty(message = "At least one checkout item is required")
    private List<CheckoutPaymentItemRequest> items;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Shipping city is required")
    private String shippingCity;

    @NotBlank(message = "Shipping state is required")
    private String shippingState;

    @NotBlank(message = "Shipping pincode is required")
    private String shippingPincode;

    @NotBlank(message = "Customer phone is required")
    private String customerPhone;

    private String paymentMethod;
}