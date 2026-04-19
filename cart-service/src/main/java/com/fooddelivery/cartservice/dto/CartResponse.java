package com.fooddelivery.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private Long cartId;
    private String customerId;
    private Long restaurantId;
    private Double totalPrice;
    private List<CartItemResponse> items;
    private int itemCount;
}
