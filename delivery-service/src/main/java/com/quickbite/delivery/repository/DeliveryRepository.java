package com.quickbite.delivery.repository;

import com.quickbite.delivery.entity.DeliveryAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<DeliveryAgent, Long> {

    Optional<DeliveryAgent> findByUserId(Long userId);

    Optional<DeliveryAgent> findByAgentId(Long agentId);

        List<DeliveryAgent> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);

    List<DeliveryAgent> findByIsAvailable(Boolean isAvailable);

    List<DeliveryAgent> findByIsVerified(Boolean isVerified);

    long countByIsAvailable(Boolean isAvailable);

    void deleteByAgentId(Long agentId);

    Optional<DeliveryAgent> findByPhone(String phone);

    boolean existsByUserId(Long userId);

    boolean existsByPhone(String phone);

    /** Agents currently on an active delivery */
    List<DeliveryAgent> findByCurrentOrderIdIsNotNull();

    /**
     * Haversine formula — find available, verified agents within radiusKm.
     *
     * Formula:
     *   distance = 2 * R * ASIN(SQRT(
     *       POWER(SIN(RADIANS(lat2 - lat1) / 2), 2) +
     *       COS(RADIANS(lat1)) * COS(RADIANS(lat2)) *
     *       POWER(SIN(RADIANS(lon2 - lon1) / 2), 2)
     *   ))
     * where R = 6371 km (Earth radius)
     */
    @Query("""
            SELECT a FROM DeliveryAgent a
            WHERE a.isAvailable = true
              AND a.isVerified  = true
              AND a.currentOrderId IS NULL
              AND a.currentLatitude  IS NOT NULL
              AND a.currentLongitude IS NOT NULL
              AND (6371 * 2 * ASIN(SQRT(
                    POWER(SIN(RADIANS(a.currentLatitude  - :lat) / 2), 2)
                  + COS(RADIANS(:lat)) * COS(RADIANS(a.currentLatitude))
                  * POWER(SIN(RADIANS(a.currentLongitude - :lon) / 2), 2)
                  ))) <= :radiusKm
            ORDER BY (6371 * 2 * ASIN(SQRT(
                    POWER(SIN(RADIANS(a.currentLatitude  - :lat) / 2), 2)
                  + COS(RADIANS(:lat)) * COS(RADIANS(a.currentLatitude))
                  * POWER(SIN(RADIANS(a.currentLongitude - :lon) / 2), 2)
                  ))) ASC
            """)
    List<DeliveryAgent> findNearbyAvailableAgents(
            @Param("lat") Double lat,
            @Param("lon") Double lon,
            @Param("radiusKm") Double radiusKm
    );
}
