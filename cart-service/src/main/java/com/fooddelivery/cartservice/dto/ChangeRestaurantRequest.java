package com.fooddelivery.cartservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRestaurantRequest {

    @NotNull(message = "restaurantId is required")
    private Long restaurantId;
}
