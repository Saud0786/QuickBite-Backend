package com.quickbite.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Trigger a refund for a paid order */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    private String reason = "Order cancelled by customer";
}
