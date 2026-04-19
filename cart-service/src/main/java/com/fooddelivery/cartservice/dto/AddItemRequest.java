package com.fooddelivery.cartservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddItemRequest {

    @NotNull(message = "restaurantId is required")
    private Long restaurantId;

    @NotNull(message = "menuItemId is required")
    private Long menuItemId;

    @NotBlank(message = "Item name is required")
    private String name;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be non-negative")
    private Double price;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private String customization;
}
