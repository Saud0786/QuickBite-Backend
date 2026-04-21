package com.quickbite.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Step 1 — Frontend calls this to create a Razorpay Order before opening
 * the Razorpay checkout widget.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    @NotNull(message = "customerId is required")
    private String customerId;

    @NotNull(message = "amount is required")
    @Min(value = 1, message = "Amount must be at least ₹1")
    private Double amount;

    /**
     * CARD / UPI  — only online modes go through Razorpay.
     * WALLET and COD are handled directly without Razorpay.
     */
    @NotBlank(message = "mode is required")
    private String mode;

    private String currency = "INR";
}
