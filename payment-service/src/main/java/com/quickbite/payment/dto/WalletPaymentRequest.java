package com.quickbite.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Pay for an order directly from the wallet balance */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletPaymentRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    @NotNull(message = "customerId is required")
    private String customerId;

    @NotNull(message = "amount is required")
    @Min(value = 1, message = "Amount must be at least ₹1")
    private Double amount;
}
