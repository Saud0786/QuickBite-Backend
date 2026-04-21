package com.quickbite.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DeliveryAgent — profile and real-time state of a delivery agent.
 *
 * Admin must set isVerified = true before agent can receive orders.
 * Agent must set isAvailable = true to appear in nearby-agent queries.
 * currentLatitude / currentLongitude are updated on every location ping.
 */
@Entity
@Table(name = "delivery_agents", indexes = {
        @Index(name = "idx_agent_user",      columnList = "userId",      unique = true),
    @Index(name = "idx_agent_restaurant", columnList = "restaurantId"),
        @Index(name = "idx_agent_available", columnList = "isAvailable"),
        @Index(name = "idx_agent_verified",  columnList = "isVerified")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DeliveryAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long agentId;

    /** Links to the auth-service User record */
    @Column(nullable = false, unique = true)
    private Long userId;

    /** Restaurant that registered the agent */
    private Long restaurantId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    /** e.g. BIKE / BICYCLE / SCOOTER / CAR */
    @Column(nullable = false)
    private String vehicleType;

    @Column(nullable = false)
    private String vehicleNumber;

    /** Live GPS coordinates — updated on every location ping */
    private Double currentLatitude;
    private Double currentLongitude;

    /** Agent toggles this to go online/offline */
    @Column(nullable = false)
    private Boolean isAvailable = false;

    /** Admin sets this after identity + document verification */
    @Column(nullable = false)
    private Boolean isVerified = false;

    /** Computed average of all delivery ratings received */
    @Column(nullable = false)
    private Double avgRating = 0.0;

    /** Total completed deliveries */
    @Column(nullable = false)
    private Integer totalDeliveries = 0;

    /** Currently assigned orderId — null when free */
    private Long currentOrderId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime locationUpdatedAt;

    // ── Business helpers ──────────────────────────────────────────────────────

    public boolean isReadyForAssignment() {
        return Boolean.TRUE.equals(isVerified)
                && Boolean.TRUE.equals(isAvailable)
                && currentOrderId == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeliveryAgent a)) return false;
        return Objects.equals(agentId, a.agentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentId);
    }
}
