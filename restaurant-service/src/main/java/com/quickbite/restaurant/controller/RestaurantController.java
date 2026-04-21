package com.quickbite.restaurant.controller;

import com.quickbite.restaurant.dto.*;
import com.quickbite.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RestaurantController — REST API layer for Restaurant-Service.
 *
 * Base path: /api/restaurants
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  PUBLIC  (no JWT)                                                       │
 * │  GET  /api/restaurants                  → list all active               │
 * │  GET  /api/restaurants/{id}             → get by id                     │
 * │  GET  /api/restaurants/search?q=        → keyword search                │
 * │  GET  /api/restaurants/nearby           → geo-proximity search          │
 * │  GET  /api/restaurants/city/{city}      → by city                       │
 * │  GET  /api/restaurants/cuisine/{c}      → by cuisine                    │
 * │  GET  /api/restaurants/cuisines         → distinct cuisine list         │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │  OWNER  (JWT, ROLE_OWNER)                                               │
 * │  POST /api/restaurants                  → register restaurant           │
 * │  PUT  /api/restaurants/{id}             → update own restaurant         │
 * │  PUT  /api/restaurants/{id}/toggle-open → open / close                  │
 * │  GET  /api/restaurants/my               → my restaurants                │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │  ADMIN  (JWT, ROLE_ADMIN)                                               │
 * │  GET  /api/restaurants/all              → all restaurants (admin)       │
 * │  GET  /api/restaurants/pending          → pending approval list         │
 * │  PUT  /api/restaurants/{id}/approve     → approve                       │
 * │  PUT  /api/restaurants/{id}/reject      → reject with reason            │
 * │  DELETE /api/restaurants/{id}           → hard delete                   │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │  INTERNAL  (called by Review-Service)                                   │
 * │  PUT  /api/restaurants/{id}/rating      → update rolling avg rating     │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Restaurant", description = "Restaurant management and discovery APIs")
public class RestaurantController {

    private final RestaurantService restaurantService;

    // ══════════════════════════════════════════════════════════
    //  PUBLIC ENDPOINTS (accessible by Guest + Customer)
    // ══════════════════════════════════════════════════════════

    /**
     * GET /api/restaurants
     * Returns all approved + open restaurants (customer-facing home feed).
     */
    @GetMapping
    @Operation(summary = "List all active (approved + open) restaurants")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getActiveRestaurants() {
        List<RestaurantResponse> restaurants = restaurantService.getByCity(""); // fallback: all
        // Actually return all approved+open — city filter is separate
        List<RestaurantResponse> all = restaurantService.getAllRestaurants()
                .stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsApproved()) && Boolean.TRUE.equals(r.getIsOpen()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(all, "Restaurants fetched"));
    }

    /**
     * GET /api/restaurants/{id}
     * Get a single restaurant by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get restaurant by ID")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(restaurantService.getById(id), "Restaurant found"));
    }

    /**
     * GET /api/restaurants/search?q=keyword
     * Keyword search across name, cuisine, city.
     */
    @GetMapping("/search")
    @Operation(summary = "Search restaurants by keyword")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> search(
            @RequestParam String q) {
        return ResponseEntity.ok(
                ApiResponse.success(restaurantService.searchRestaurants(q), "Search results"));
    }

    /**
     * GET /api/restaurants/nearby?lat=&lng=&radius=
     * Geo-proximity search using Haversine formula.
     */
    @GetMapping("/nearby")
    @Operation(summary = "Find nearby restaurants by GPS coordinates")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getNearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "10.0") Double radius) {
        return ResponseEntity.ok(
                ApiResponse.success(restaurantService.getNearby(lat, lng, radius),
                        "Nearby restaurants fetched"));
    }

    /**
     * GET /api/restaurants/city/{city}
     * Get approved + open restaurants in a city.
     */
    @GetMapping("/city/{city}")
    @Operation(summary = "Get restaurants by city")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getByCity(
            @PathVariable String city) {
        return ResponseEntity.ok(
                ApiResponse.success(restaurantService.getByCity(city),
                        "Restaurants in " + city));
    }

    /**
     * GET /api/restaurants/cuisine/{cuisine}
     * Get restaurants by cuisine type.
     */
    @GetMapping("/cuisine/{cuisine}")
    @Operation(summary = "Get restaurants by cuisine")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getByCuisine(
            @PathVariable String cuisine) {
        return ResponseEntity.ok(
                ApiResponse.success(restaurantService.getByCuisine(cuisine),
                        cuisine + " restaurants fetched"));
    }

    /**
     * GET /api/restaurants/cuisines
     * Get distinct list of all available cuisines (for filter chips in UI).
     */
    @GetMapping("/cuisines")
    @Operation(summary = "Get all available cuisine types")
    public ResponseEntity<ApiResponse<List<String>>> getDistinctCuisines() {
        return ResponseEntity.ok(
                ApiResponse.success(restaurantService.getDistinctCuisines(),
                        "Cuisines fetched"));
    }

    // ══════════════════════════════════════════════════════════
    //  OWNER ENDPOINTS (requires ROLE_OWNER JWT)
    // ══════════════════════════════════════════════════════════

    /**
     * POST /api/restaurants
     * Register a new restaurant. Requires OWNER role.
     * Sets isApproved=false — awaits admin approval before going live.
     */
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Register a new restaurant",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<RestaurantResponse>> register(
            @Valid @ModelAttribute RestaurantRequest request,
            Authentication authentication) {

        String ownerId = extractUserId(authentication);
        RestaurantResponse response = restaurantService.registerRestaurant(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response,
                        "Restaurant registered successfully. Awaiting admin approval."));
    }

    /**
     * GET /api/restaurants/my
     * Get all restaurants belonging to the logged-in owner.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Get my restaurants",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getMyRestaurants(
            Authentication authentication) {

        String ownerId = extractUserId(authentication);
                String principalName = authentication.getName();

                List<RestaurantResponse> primary = restaurantService.getByOwner(ownerId);
                List<RestaurantResponse> merged;

                // Backward compatibility: older rows may have ownerId stored as email.
                if (principalName != null && !principalName.isBlank() && !principalName.equals(ownerId)) {
                        List<RestaurantResponse> fallback = restaurantService.getByOwner(principalName);
                        Map<Integer, RestaurantResponse> byId = new LinkedHashMap<>();
                        primary.forEach(r -> byId.put(r.getRestaurantId(), r));
                        fallback.forEach(r -> byId.putIfAbsent(r.getRestaurantId(), r));
                        merged = byId.values().stream().toList();
                } else {
                        merged = primary;
                }

        return ResponseEntity.ok(
                                ApiResponse.success(merged,
                        "Your restaurants fetched"));
    }

    /**
     * PUT /api/restaurants/{id}
     * Update restaurant profile (owner only, must be their own restaurant).
     */
    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Update restaurant profile",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<RestaurantResponse>> update(
            @PathVariable Integer id,
            @Valid @ModelAttribute RestaurantRequest request,
            Authentication authentication) {

        String requesterId = extractUserId(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(restaurantService.updateRestaurant(id, request, requesterId),
                        "Restaurant updated successfully"));
    }

    /**
     * PUT /api/restaurants/{id}/toggle-open
     * Toggle the restaurant's open/closed state.
     */
    @PutMapping("/{id}/toggle-open")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Toggle restaurant open/close status",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<RestaurantResponse>> toggleOpen(
            @PathVariable Integer id,
            Authentication authentication) {

        String ownerId = extractUserId(authentication);
        RestaurantResponse response = restaurantService.toggleOpen(id, ownerId);
        String status = Boolean.TRUE.equals(response.getIsOpen()) ? "OPEN" : "CLOSED";
        return ResponseEntity.ok(
                ApiResponse.success(response, "Restaurant is now " + status));
    }

    // ══════════════════════════════════════════════════════════
    //  ADMIN ENDPOINTS (requires ROLE_ADMIN JWT)
    // ══════════════════════════════════════════════════════════

    /**
     * GET /api/restaurants/all
     * Get all restaurants regardless of status (admin dashboard).
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Get all restaurants",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(restaurantService.getAllRestaurants(),
                        "All restaurants fetched"));
    }

    /**
     * GET /api/restaurants/pending
     * Get all restaurants pending admin approval.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Get restaurants pending approval",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getPending() {
        return ResponseEntity.ok(
                ApiResponse.success(restaurantService.getPendingApproval(),
                        "Pending restaurants fetched"));
    }

    /**
     * PUT /api/restaurants/{id}/approve
     * Approve a restaurant registration (admin only).
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Approve restaurant",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<RestaurantResponse>> approve(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(restaurantService.approveRestaurant(id),
                        "Restaurant approved successfully"));
    }


    /**
     * DELETE /api/restaurants/{id}
     * Hard-delete a restaurant from the platform (admin only).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Delete restaurant",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Restaurant deleted successfully"));
    }

    /**
     * GET /api/restaurants/owner/{ownerId}
     * [Admin] Get all restaurants belonging to a specific owner.
     */
    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Get restaurants by owner",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getByOwner(
            @PathVariable String ownerId) {
        return ResponseEntity.ok(
                ApiResponse.success(restaurantService.getByOwner(ownerId),
                        "Owner's restaurants fetched"));
    }

    // ══════════════════════════════════════════════════════════
    //  INTERNAL (called by Review-Service)
    // ══════════════════════════════════════════════════════════

    /**
     * PUT /api/restaurants/{id}/rating
     * Update rolling average rating — called internally by Review-Service after
     * each new food rating is submitted by a customer.
     *
     * Protected by JWT; any authenticated service can call this.
     */
    @PutMapping("/{id}/rating")
    @Operation(summary = "[Internal] Update restaurant average rating",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateRating(
            @PathVariable Integer id,
            @Valid @RequestBody RatingUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(restaurantService.updateRating(id, request),
                        "Rating updated"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Extract the userId stored in Authentication.details by JwtAuthenticationFilter.
     * Falls back to the principal (email) if details is null.
     */
    private String extractUserId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof String userId && !userId.isBlank()) {
            return userId;
        }
        // Fallback: use email as identifier (matches ownerId in some implementations)
        return authentication.getName();
    }
}
