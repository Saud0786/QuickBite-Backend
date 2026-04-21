package com.quickbite.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Mirrors the cart-service CartResponse shape so Feign can deserialize it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private String status;
    private String message;
    private CartData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartData {
        private Long cartId;
        private String customerId;
        private Long restaurantId;
        private Double totalPrice;
        private List<CartItemData> items;
        private int itemCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemData {
        private Long itemId;
        private Long menuItemId;
        private String name;
        private Double price;
        private Integer quantity;
        private String customization;
        private Double subtotal;
    }
}
