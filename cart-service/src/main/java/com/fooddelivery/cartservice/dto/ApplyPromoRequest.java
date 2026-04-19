package com.fooddelivery.cartservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyPromoRequest {

    @NotBlank(message = "Promo code is required")
    private String promoCode;
}
