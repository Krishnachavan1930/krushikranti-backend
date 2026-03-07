package com.krushikranti.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespondDealRequest {

    @NotNull(message = "Deal offer ID is required")
    private Long dealOfferId;

    @NotBlank(message = "Action is required (ACCEPT or REJECT)")
    private String action; // ACCEPT or REJECT
}
