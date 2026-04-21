package com.quickbite.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearbyAgentResponse {
    private Long agentId;
    private String fullName;
    private String phone;
    private String vehicleType;
    private Double currentLatitude;
    private Double currentLongitude;
    private Double avgRating;
    private Integer totalDeliveries;
    private Double distanceKm;     // computed Haversine distance from query point
}
