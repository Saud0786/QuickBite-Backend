package com.quickbite.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {

    @NotBlank(message = "customerId is required")
    private String customerId;

    @NotBlank(message = "deliveryAddress is required")
    private String deliveryAddress;

    @NotBlank(message = "modeOfPayment is required")
    private String modeOfPayment;   // COD / CARD / UPI / WALLET

    private String specialInstructions;
}
