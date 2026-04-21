package com.quickbite.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Record a Cash on Delivery payment (no Razorpay involved) */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CODPaymentRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    @NotNull(message = "customerId is required")
    private String customerId;

    @NotNull(message = "amount is required")
    @Min(value = 1, message = "Amount must be positive")
    private Double amount;
}
