package com.quickbite.restaurant.repository;

import com.quickbite.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RestaurantRepository — data access layer.
 *
 * All methods from the case study class diagram are implemented here,
 * plus the Haversine formula for geo-proximity search.
 */
@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Integer> {

    // ── Standard finders ─────────────────────────────────────────────────────

    List<Restaurant> findByOwnerId(String ownerId);

    List<Restaurant> findByCuisineIgnoreCase(String cuisine);

    List<Restaurant> findByCityIgnoreCase(String city);

    List<Restaurant> findByIsOpenAndIsApproved(Boolean isOpen, Boolean isApproved);

    List<Restaurant> findByCityIgnoreCaseAndIsOpenAndIsApproved(
            String city, Boolean isOpen, Boolean isApproved);

    Optional<Restaurant> findByRestaurantId(Integer restaurantId);

    boolean existsByOwnerIdAndName(String ownerId, String name);

    // ── Name search ───────────────────────────────────────────────────────────

    @Query("SELECT r FROM Restaurant r WHERE " +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.cuisine) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.city) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Restaurant> searchByName(@Param("keyword") String keyword);

    @Query("SELECT r FROM Restaurant r WHERE " +
           "r.isApproved = true AND r.isOpen = true AND (" +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.cuisine) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
           "OR LOWER(r.city) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Restaurant> searchActiveByKeyword(@Param("keyword") String keyword);

    // ── Geo-proximity (Haversine formula) ─────────────────────────────────────
    /**
     * Find restaurants within {@code radiusKm} kilometres of the given lat/lng.
     * Uses the Haversine formula entirely in JPQL/SQL for accuracy.
     *
     * Formula: distance = 2 * R * ASIN(SQRT(
     *   POWER(SIN((lat2-lat1) * PI/360), 2) +
     *   COS(lat1*PI/180) * COS(lat2*PI/180) * POWER(SIN((lng2-lng1)*PI/360), 2)
     * ))
     * where R = 6371 km.
     */
    @Query(value = """
        SELECT r.*, (
            6371 * 2 * ASIN(SQRT(
                POWER(SIN((:lat - r.latitude) * PI() / 360), 2) +
                COS(:lat * PI() / 180) * COS(r.latitude * PI() / 180) *
                POWER(SIN((:lng - r.longitude) * PI() / 360), 2)
            ))
        ) AS distance_km
        FROM restaurants r
        WHERE r.is_approved = true
          AND r.is_open = true
          AND (
            6371 * 2 * ASIN(SQRT(
                POWER(SIN((:lat - r.latitude) * PI() / 360), 2) +
                COS(:lat * PI() / 180) * COS(r.latitude * PI() / 180) *
                POWER(SIN((:lng - r.longitude) * PI() / 360), 2)
            ))
          ) <= :radiusKm
        ORDER BY distance_km ASC
        """, nativeQuery = true)
    List<Restaurant> findNearbyRestaurants(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusKm") Double radiusKm);

    /**
     * Find ALL restaurants within radius regardless of approval/open status
     * (used by admin for area analytics).
     */
    @Query(value = """
        SELECT r.* FROM restaurants r
        WHERE (
            6371 * 2 * ASIN(SQRT(
                POWER(SIN((:lat - r.latitude) * PI() / 360), 2) +
                COS(:lat * PI() / 180) * COS(r.latitude * PI() / 180) *
                POWER(SIN((:lng - r.longitude) * PI() / 360), 2)
            ))
        ) <= :radiusKm
        ORDER BY r.avg_rating DESC
        """, nativeQuery = true)
    List<Restaurant> findAllNearby(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusKm") Double radiusKm);

    // ── Counts ────────────────────────────────────────────────────────────────

    long countByCityIgnoreCase(String city);

    long countByIsApprovedFalse();

    long countByIsApprovedTrue();

    // ── Rating update ─────────────────────────────────────────────────────────

    /**
     * Atomically update the average rating for a restaurant.
     * Called by Review-Service after each new review submission.
     */
    @Modifying
    @Query("UPDATE Restaurant r SET r.avgRating = :avgRating WHERE r.restaurantId = :restaurantId")
    void updateRating(
            @Param("restaurantId") Integer restaurantId,
            @Param("avgRating") Double avgRating);

    // ── Admin pending approval ────────────────────────────────────────────────

    List<Restaurant> findByIsApprovedFalse();

    // ── Cuisine list (distinct) ───────────────────────────────────────────────

    @Query("SELECT DISTINCT r.cuisine FROM Restaurant r WHERE r.isApproved = true ORDER BY r.cuisine")
    List<String> findDistinctCuisines();
}
