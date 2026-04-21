package com.quickbite.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Add money to wallet via Razorpay (CARD / UPI) */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddMoneyRequest {

    @NotNull(message = "customerId is required")
    private String customerId;

    @NotNull(message = "amount is required")
    @Min(value = 1, message = "Minimum top-up is ₹1")
    private Double amount;

    private String currency = "INR";
}
