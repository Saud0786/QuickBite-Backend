package com.quickbite.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private Long restaurantId;
    private Long deliveryAgentId;
    private Double totalAmount;
    private Double discount;
    private Double finalAmount;
    private String modeOfPayment;
    private String orderStatus;
    private LocalDateTime orderDate;
    private LocalDateTime estimatedDelivery;
    private String deliveryAddress;
    private String specialInstructions;
    private List<OrderItemResponse> items;
    private int itemCount;
}
