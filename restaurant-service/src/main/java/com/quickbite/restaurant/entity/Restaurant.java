package com.quickbite.restaurant.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Restaurant Entity — core domain object for the Restaurant-Service.
 *
 * Stores full restaurant profile including GPS coordinates for
 * geo-proximity search, cuisine tags, admin approval state,
 * delivery configuration, and aggregated ratings.
 */
@Entity
@Table(name = "restaurants", indexes = {
        @Index(name = "idx_owner_id",   columnList = "owner_id"),
        @Index(name = "idx_cuisine",    columnList = "cuisine"),
        @Index(name = "idx_city",       columnList = "city"),
        @Index(name = "idx_is_open",    columnList = "is_open"),
        @Index(name = "idx_is_approved",columnList = "is_approved"),
        @Index(name = "idx_lat_lng",    columnList = "latitude, longitude")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restaurant_id")
    private Integer restaurantId;

    /**
     * References User.userId from auth-service (owner's UUID).
     */
    @NotBlank(message = "Owner ID is required")
    @Column(name = "owner_id", nullable = false, length = 50)
    private String ownerId;

    @NotBlank(message = "Restaurant name is required")
    @Size(min = 2, max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Cuisine is required")
    @Column(name = "cuisine", nullable = false, length = 100)
    private String cuisine;

    @NotBlank(message = "Address is required")
    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @NotBlank(message = "City is required")
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Aggregated average food rating — updated by Review-Service callback.
     * Default 0.0 until first review is submitted.
     */
    @Column(name = "avg_rating", nullable = false)
    @Builder.Default
    private Double avgRating = 0.0;

    @Column(name = "total_ratings", nullable = false)
    @Builder.Default
    private Integer totalRatings = 0;


    /**
     * Whether the restaurant is currently accepting orders.
     */
    @Column(name = "is_open", nullable = false)
    @Builder.Default
    private Boolean isOpen = false;

    /**
     * Whether admin has approved this restaurant.
     * Only approved + open restaurants appear in customer search.
     */
    @Column(name = "is_approved", nullable = false)
    @Builder.Default
    private Boolean isApproved = false;

    /**
     * Delivery radius in kilometres from the restaurant's GPS location.
     */
    @Column(name = "delivery_radius", nullable = false)
    @Builder.Default
    private Double deliveryRadius = 5.0;

    /**
     * Minimum order amount in INR for this restaurant.
     */
    @Column(name = "min_order_amount", nullable = false)
    @Builder.Default
    private Double minOrderAmount = 0.0;

    /**
     * Estimated delivery time in minutes.
     */
    @Column(name = "estimated_delivery_min", nullable = false)
    @Builder.Default
    private Integer estimatedDeliveryMin = 30;


    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
