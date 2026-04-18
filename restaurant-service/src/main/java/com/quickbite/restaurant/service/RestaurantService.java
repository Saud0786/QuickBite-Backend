package com.quickbite.restaurant.service;

import com.quickbite.restaurant.dto.RatingUpdateRequest;
import com.quickbite.restaurant.dto.RestaurantRequest;
import com.quickbite.restaurant.dto.RestaurantResponse;

import java.util.List;

/**
 * RestaurantService — business contract for the Restaurant-Service.
 * All methods from the case study class diagram are declared here.
 */
public interface RestaurantService {

    // ── Registration & CRUD ───────────────────────────────────────────────────

    /** Register a new restaurant (submitted by an Owner, awaits admin approval). */
    RestaurantResponse registerRestaurant(RestaurantRequest request, String ownerId);

    /** Get a restaurant by its primary key. */
    RestaurantResponse getById(Integer restaurantId);

    /** Get all restaurants owned by a specific owner (by ownerId from auth-service). */
    List<RestaurantResponse> getByOwner(String ownerId);

    /** Get restaurants filtered by cuisine type. */
    List<RestaurantResponse> getByCuisine(String cuisine);

    /** Get approved + open restaurants in a city. */
    List<RestaurantResponse> getByCity(String city);

    /** Update an existing restaurant's profile (owner or admin). */
    RestaurantResponse updateRestaurant(Integer restaurantId, RestaurantRequest request, String requesterId);

    /** Hard-delete a restaurant (admin only). */
    void deleteRestaurant(Integer restaurantId);

    // ── Geo-Proximity ─────────────────────────────────────────────────────────

    /**
     * Find approved + open restaurants within {@code radiusKm} of the given
     * GPS coordinates, sorted by distance ascending.
     */
    List<RestaurantResponse> getNearby(Double latitude, Double longitude, Double radiusKm);

    // ── Search ────────────────────────────────────────────────────────────────

    /** Full-text keyword search across name, cuisine, city (active restaurants only). */
    List<RestaurantResponse> searchRestaurants(String keyword);

    // ── Admin Operations ──────────────────────────────────────────────────────

    /** Approve a pending restaurant registration (admin only). */
    RestaurantResponse approveRestaurant(Integer restaurantId);



    /** Get all restaurants pending admin approval. */
    List<RestaurantResponse> getPendingApproval();

    // ── Open/Close Toggle ─────────────────────────────────────────────────────

    /** Toggle the restaurant's isOpen flag (owner only). */
    RestaurantResponse toggleOpen(Integer restaurantId, String ownerId);

    // ── Rating ────────────────────────────────────────────────────────────────

    /**
     * Update the aggregated average rating.
     * Called internally by the Review-Service after each new rating submission.
     *
     * @param restaurantId Target restaurant
     * @param request      Contains the new individual rating to fold in
     */
    RestaurantResponse updateRating(Integer restaurantId, RatingUpdateRequest request);

    // ── Analytics ────────────────────────────────────────────────────────────

    /** List all distinct cuisine categories available on the platform. */
    List<String> getDistinctCuisines();

    /** Get all restaurants (admin dashboard). */
    List<RestaurantResponse> getAllRestaurants();
}
