package com.quickbite.restaurant.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full restaurant profile response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor 
@Builder
public class RestaurantResponse {
    private Integer restaurantId;
    private String ownerId;
    private String name;
    private String description;
    private String cuisine;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String imageUrl;
    private Boolean isOpen;
    private Boolean isApproved;
    private Double deliveryRadius;
    private Double minOrderAmount;
    private Integer estimatedDeliveryMin;
    private Double avgRating;
    private Integer totalRatings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Distance in km from searched location — populated only for geo-proximity results */
    private Double distanceKm;
}

