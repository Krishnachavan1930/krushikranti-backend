package com.krushikranti.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartNegotiationRequest {

    @NotNull(message = "Bulk product ID is required")
    private Long bulkProductId;
}
