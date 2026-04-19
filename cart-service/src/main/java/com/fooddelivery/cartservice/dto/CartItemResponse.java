package com.fooddelivery.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long itemId;
    private Long menuItemId;
    private String name;
    private Double price;
    private Integer quantity;
    private String customization;
    private Double subtotal;
}
