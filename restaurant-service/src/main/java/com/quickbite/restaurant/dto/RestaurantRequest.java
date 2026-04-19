package com.quickbite.restaurant.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

/* ═══════════════════════════════════════════════════════════
   REQUEST DTOs
   ═══════════════════════════════════════════════════════════ */

/**
 * Payload for registering or updating a restaurant.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RestaurantRequest {

    @NotBlank(message = "Restaurant name is required")
    @Size(min = 2, max = 150)
    private String name;

    private String description;

    @NotBlank(message = "Cuisine is required")
    private String cuisine;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotNull(message = "Latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitude;

    private String phone;

    @PositiveOrZero
    private Double deliveryRadius;

    @PositiveOrZero
    private Double minOrderAmount;

    @Positive
    private Integer estimatedDeliveryMin;

    private org.springframework.web.multipart.MultipartFile image;

}

