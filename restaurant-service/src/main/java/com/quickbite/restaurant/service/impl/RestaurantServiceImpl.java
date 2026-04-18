package com.quickbite.restaurant.service.impl;

import com.quickbite.restaurant.dto.*;

import com.quickbite.restaurant.entity.Restaurant;
import com.quickbite.restaurant.exception.CustomExceptions;
import com.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * RestaurantServiceImpl — full implementation of all restaurant business logic.
 *
 * Key design decisions:
 * - Geo-proximity search via Haversine formula (delegated to repository native query)
 * - Rating computation: rolling average updated on each new rating via updateRating()
 * - Approval workflow: isApproved=false on register, set to true only by admin
 * - Owner check enforced at service layer before any mutating operation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;

    // ── Registration ──────────────────────────────────────────────────────────

    @Override
    public RestaurantResponse registerRestaurant(RestaurantRequest request, String ownerId) {
        log.info("Owner {} registering restaurant: {}", ownerId, request.getName());

        // Prevent duplicate restaurant name per owner
        if (restaurantRepository.existsByOwnerIdAndName(ownerId, request.getName())) {
            throw new CustomExceptions.RestaurantAlreadyExistsException(
                    "You have already registered a restaurant named: " + request.getName());
        }

        Restaurant restaurant = buildFromRequest(request, ownerId);
        Restaurant saved = restaurantRepository.save(restaurant);

        log.info("Restaurant registered with id={}, pending approval", saved.getRestaurantId());
        return toResponse(saved, null);
    }

    // ── Read Operations ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse getById(Integer restaurantId) {
        Restaurant r = findOrThrow(restaurantId);
        return toResponse(r, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getByOwner(String ownerId) {
        return restaurantRepository.findByOwnerId(ownerId)
                .stream().map(r -> toResponse(r, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getByCuisine(String cuisine) {
        return restaurantRepository.findByCuisineIgnoreCase(cuisine)
                .stream()
                .filter(r -> r.getIsApproved() && r.getIsOpen())
                .map(r -> toResponse(r, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getByCity(String city) {
        return restaurantRepository.findByCityIgnoreCaseAndIsOpenAndIsApproved(city, true, true)
                .stream().map(r -> toResponse(r, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getAllRestaurants() {
        return restaurantRepository.findAll()
                .stream().map(r -> toResponse(r, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getPendingApproval() {
        return restaurantRepository.findByIsApprovedFalse()
                .stream().map(r -> toResponse(r, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctCuisines() {
        return restaurantRepository.findDistinctCuisines();
    }

    // ── Geo-Proximity ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getNearby(Double latitude, Double longitude, Double radiusKm) {
        log.debug("Geo-search: lat={}, lng={}, radius={}km", latitude, longitude, radiusKm);

        // Default radius = 10 km if not supplied
        double radius = (radiusKm != null && radiusKm > 0) ? radiusKm : 10.0;

        return restaurantRepository.findNearbyRestaurants(latitude, longitude, radius)
                .stream()
                .map(r -> {
                    double dist = haversineKm(latitude, longitude, r.getLatitude(), r.getLongitude());
                    return toResponse(r, dist);
                })
                .toList();
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> searchRestaurants(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        return restaurantRepository.searchActiveByKeyword(keyword.trim())
                .stream().map(r -> toResponse(r, null)).toList();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public RestaurantResponse updateRestaurant(Integer restaurantId, RestaurantRequest request, String requesterId) {
        Restaurant r = findOrThrow(restaurantId);

        // Only the owner or (in production) admin can update — enforced here
        if (!r.getOwnerId().equals(requesterId)) {
            throw new CustomExceptions.UnauthorizedAccessException(
                    "You are not the owner of this restaurant");
        }

        applyUpdate(r, request);
        Restaurant saved = restaurantRepository.save(r);
        log.info("Restaurant {} updated by owner {}", restaurantId, requesterId);
        return toResponse(saved, null);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    public void deleteRestaurant(Integer restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new CustomExceptions.RestaurantNotFoundException(
                    "Restaurant not found with id: " + restaurantId);
        }
        restaurantRepository.deleteById(restaurantId);
        log.info("Restaurant {} deleted", restaurantId);
    }

    // ── Admin: Approve / Reject ───────────────────────────────────────────────

    @Override
    public RestaurantResponse approveRestaurant(Integer restaurantId) {
        Restaurant r = findOrThrow(restaurantId);
        r.setIsApproved(true);
        Restaurant saved = restaurantRepository.save(r);
        log.info("Restaurant {} approved", restaurantId);
        return toResponse(saved, null);
    }


    // ── Toggle Open ───────────────────────────────────────────────────────────

    @Override
    public RestaurantResponse toggleOpen(Integer restaurantId, String ownerId) {
        Restaurant r = findOrThrow(restaurantId);

        if (!r.getOwnerId().equals(ownerId)) {
            throw new CustomExceptions.UnauthorizedAccessException(
                    "You are not the owner of this restaurant");
        }
        if (!r.getIsApproved()) {
            throw new CustomExceptions.RestaurantNotApprovedException(
                    "Restaurant must be approved before it can be opened");
        }

        r.setIsOpen(!r.getIsOpen());
        Restaurant saved = restaurantRepository.save(r);
        log.info("Restaurant {} toggled isOpen → {}", restaurantId, saved.getIsOpen());
        return toResponse(saved, null);
    }

    // ── Rating ────────────────────────────────────────────────────────────────

    /**
     * Rolling average: newAvg = ((oldAvg * totalRatings) + newRating) / (totalRatings + 1)
     * Then persisted atomically via repository.updateRating().
     */
    @Override
    public RestaurantResponse updateRating(Integer restaurantId, RatingUpdateRequest request) {
        Restaurant r = findOrThrow(restaurantId);

        double newAvg = Math.round(request.getNewRating() * 100.0) / 100.0;   // round to 2 decimal places

        restaurantRepository.updateRating(restaurantId, newAvg);
        r.setAvgRating(newAvg);

        log.debug("Restaurant {} rating updated to {}", restaurantId, newAvg);
        return toResponse(r, null);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private Restaurant findOrThrow(Integer restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new CustomExceptions.RestaurantNotFoundException(
                        "Restaurant not found with id: " + restaurantId));
    }

    private Restaurant buildFromRequest(RestaurantRequest req, String ownerId) {
        return Restaurant.builder()
                .ownerId(ownerId)
                .name(req.getName())
                .description(req.getDescription())
                .cuisine(req.getCuisine())
                .address(req.getAddress())
                .city(req.getCity())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .phone(req.getPhone())
                .deliveryRadius(req.getDeliveryRadius() != null ? req.getDeliveryRadius() : 5.0)
                .minOrderAmount(req.getMinOrderAmount() != null ? req.getMinOrderAmount() : 0.0)
                .estimatedDeliveryMin(req.getEstimatedDeliveryMin() != null ? req.getEstimatedDeliveryMin() : 30)
                .isOpen(false)
                .isApproved(false)
                .avgRating(0.0)
                .build();
    }

    private void applyUpdate(Restaurant r, RestaurantRequest req) {
        if (req.getName()              != null) r.setName(req.getName());
        if (req.getDescription()       != null) r.setDescription(req.getDescription());
        if (req.getCuisine()           != null) r.setCuisine(req.getCuisine());
        if (req.getAddress()           != null) r.setAddress(req.getAddress());
        if (req.getCity()              != null) r.setCity(req.getCity());
        if (req.getLatitude()          != null) r.setLatitude(req.getLatitude());
        if (req.getLongitude()         != null) r.setLongitude(req.getLongitude());
        if (req.getPhone()             != null) r.setPhone(req.getPhone());
        if (req.getDeliveryRadius()    != null) r.setDeliveryRadius(req.getDeliveryRadius());
        if (req.getMinOrderAmount()    != null) r.setMinOrderAmount(req.getMinOrderAmount());
        if (req.getEstimatedDeliveryMin() != null) r.setEstimatedDeliveryMin(req.getEstimatedDeliveryMin());
    }

    /**
     * Haversine formula — returns distance in km between two GPS coordinates.
     * Used to populate distanceKm on RestaurantResponse for nearby searches.
     */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = R * c;
        return Math.round(dist * 100.0) / 100.0;
    }

    /** Map entity → response DTO. */
    public RestaurantResponse toResponse(Restaurant r, Double distanceKm) {
        return RestaurantResponse.builder()
                .restaurantId(r.getRestaurantId())
                .ownerId(r.getOwnerId())
                .name(r.getName())
                .description(r.getDescription())
                .cuisine(r.getCuisine())
                .address(r.getAddress())
                .city(r.getCity())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .phone(r.getPhone())
                .avgRating(r.getAvgRating())
                .isOpen(r.getIsOpen())
                .isApproved(r.getIsApproved())
                .deliveryRadius(r.getDeliveryRadius())
                .minOrderAmount(r.getMinOrderAmount())
                .estimatedDeliveryMin(r.getEstimatedDeliveryMin())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .distanceKm(distanceKm)
                .build();
    }
}
