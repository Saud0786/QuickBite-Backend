package com.quickbite.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAgentResponse {
    private Long agentId;
    private Long userId;
    private Long restaurantId;
    private String fullName;
    private String phone;
    private String vehicleType;
    private String vehicleNumber;
    private Double currentLatitude;
    private Double currentLongitude;
    private Boolean isAvailable;
    private Boolean isVerified;
    private Double avgRating;
    private Integer totalDeliveries;
    private Long currentOrderId;
    private LocalDateTime createdAt;
    private LocalDateTime locationUpdatedAt;
}
