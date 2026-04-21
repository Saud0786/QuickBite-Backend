package com.quickbite.delivery.resource;

import com.quickbite.delivery.dto.*;
import com.quickbite.delivery.entity.DeliveryAgent;
import com.quickbite.delivery.service.DeliveryService;
import com.quickbite.delivery.service.DeliveryServiceImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DeliveryResource — REST controller exposing /agents endpoints.
 *
 * POST   /agents/register                         → registerAgent
 * GET    /agents/{agentId}                        → getAgentById
 * GET    /agents/user/{userId}                    → getAgentByUserId
 * GET    /agents/restaurant/{restaurantId}         → getAgentsByRestaurantId
 * GET    /agents/nearby                           → getNearbyAgents (lat, lon, radius)
 * PUT    /agents/{agentId}/location               → updateLocation
 * PUT    /agents/{agentId}/availability           → setAvailability
 * PUT    /agents/{agentId}/verify                 → verifyAgent  (admin)
 * PUT    /agents/{agentId}/rating                 → updateRating
 * POST   /agents/{agentId}/assign-order           → assignOrder
 * POST   /agents/{agentId}/complete-delivery      → completeDelivery
 * GET    /agents/active-deliveries                → getActiveDeliveries
 * GET    /agents                                  → getAllAgents
 * GET    /agents/count/available                  → countAvailableAgents
 */
@RestController
@RequestMapping({"/agents", "/api/agents"})
@RequiredArgsConstructor
@Slf4j
public class DeliveryResource {

    private final DeliveryService deliveryService;

    // ─── POST /agents/register ────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> registerAgent(
            @Valid @RequestBody RegisterAgentRequest request) {
        log.info("POST /agents/register — userId={}, restaurantId={}", request.getUserId(), request.getRestaurantId());
        DeliveryAgent agent = deliveryService.registerAgent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Agent registered successfully. Awaiting admin verification.",
                        toResponse(agent)));
    }

    // ─── GET /agents/{agentId} ────────────────────────────────────────────────

    @GetMapping("/{agentId}")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> getById(
            @PathVariable Long agentId) {
        log.info("GET /agents/{}", agentId);
        DeliveryAgent agent = deliveryService.getAgentById(agentId);
        return ResponseEntity.ok(ApiResponse.success("Agent retrieved", toResponse(agent)));
    }

    // ─── GET /agents/user/{userId} ────────────────────────────────────────────

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> getByUserId(
            @PathVariable Long userId) {
        log.info("GET /agents/user/{}", userId);
        DeliveryAgent agent = deliveryService.getAgentByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Agent retrieved", toResponse(agent)));
    }

    // ─── GET /agents/restaurant/{restaurantId} ──────────────────────────────

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<ApiResponse<List<DeliveryAgentResponse>>> getByRestaurant(
            @PathVariable Long restaurantId) {
        log.info("GET /agents/restaurant/{}", restaurantId);
        List<DeliveryAgentResponse> agents = deliveryService.getAgentsByRestaurantId(restaurantId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(
                agents.size() + " agent(s) retrieved", agents));
    }

    // ─── GET /agents/nearby?lat=&lon=&radius= ─────────────────────────────────

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NearbyAgentResponse>>> getNearby(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0")   Double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") Double lon,
            @RequestParam(required = false) Double radius) {
        log.info("GET /agents/nearby — lat={}, lon={}, radius={}", lat, lon, radius);
        List<DeliveryAgent> agents = deliveryService.getNearbyAgents(lat, lon, radius);
        List<NearbyAgentResponse> response = agents.stream()
                .map(a -> toNearbyResponse(a, lat, lon))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(
                response.size() + " agent(s) found nearby", response));
    }

    // ─── PUT /agents/{agentId}/location ──────────────────────────────────────

    @PutMapping("/{agentId}/location")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> updateLocation(
            @PathVariable Long agentId,
            @Valid @RequestBody UpdateLocationRequest request) {
        log.info("PUT /agents/{}/location — lat={}, lon={}",
                agentId, request.getLatitude(), request.getLongitude());
        DeliveryAgent agent = deliveryService.updateLocation(
                agentId, request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(ApiResponse.success("Location updated", toResponse(agent)));
    }

    // ─── PUT /agents/{agentId}/availability ──────────────────────────────────

    @PutMapping("/{agentId}/availability")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> setAvailability(
            @PathVariable Long agentId,
            @RequestBody Map<String, Boolean> body) {
        Boolean available = body.get("available");
        if (available == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Field 'available' (true/false) is required."));
        }
        log.info("PUT /agents/{}/availability — available={}", agentId, available);
        DeliveryAgent agent = deliveryService.setAvailability(agentId, available);
        return ResponseEntity.ok(ApiResponse.success(
                "Availability set to " + available, toResponse(agent)));
    }

    // ─── PUT /agents/{agentId}/verify  (admin) ────────────────────────────────

    @PutMapping("/{agentId}/verify")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> verifyAgent(
            @PathVariable Long agentId) {
        log.info("PUT /agents/{}/verify", agentId);
        DeliveryAgent agent = deliveryService.verifyAgent(agentId);
        return ResponseEntity.ok(ApiResponse.success(
                "Agent verified successfully", toResponse(agent)));
    }

    // ─── PUT /agents/{agentId}/rating ─────────────────────────────────────────

    @PutMapping("/{agentId}/rating")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> updateRating(
            @PathVariable Long agentId,
            @Valid @RequestBody UpdateRatingRequest request) {
        log.info("PUT /agents/{}/rating — newRating={}", agentId, request.getNewRating());
        DeliveryAgent agent = deliveryService.updateRating(agentId, request.getNewRating());
        return ResponseEntity.ok(ApiResponse.success(
                "Rating updated to " + agent.getAvgRating(), toResponse(agent)));
    }

    // ─── POST /agents/{agentId}/assign-order ─────────────────────────────────

    @PostMapping("/{agentId}/assign-order")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> assignOrder(
            @PathVariable Long agentId,
            @Valid @RequestBody AssignOrderRequest request) {
        log.info("POST /agents/{}/assign-order — orderId={}", agentId, request.getOrderId());
        DeliveryAgent agent = deliveryService.assignOrder(agentId, request.getOrderId());
        return ResponseEntity.ok(ApiResponse.success(
                "Order " + request.getOrderId() + " assigned to agent " + agentId,
                toResponse(agent)));
    }

    // ─── POST /agents/{agentId}/complete-delivery ─────────────────────────────

    @PostMapping("/{agentId}/complete-delivery")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> completeDelivery(
            @PathVariable Long agentId) {
        log.info("POST /agents/{}/complete-delivery", agentId);
        DeliveryAgent agent = deliveryService.completeDelivery(agentId);
        return ResponseEntity.ok(ApiResponse.success(
                "Delivery completed. Total deliveries: " + agent.getTotalDeliveries(),
                toResponse(agent)));
    }

    // ─── GET /agents/active-deliveries ───────────────────────────────────────

    @GetMapping("/active-deliveries")
    public ResponseEntity<ApiResponse<List<DeliveryAgentResponse>>> getActiveDeliveries() {
        log.info("GET /agents/active-deliveries");
        List<DeliveryAgentResponse> agents = deliveryService.getActiveDeliveries()
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(
                agents.size() + " active deliveries", agents));
    }

    // ─── GET /agents ──────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryAgentResponse>>> getAllAgents() {
        log.info("GET /agents");
        List<DeliveryAgentResponse> agents = deliveryService.getAllAgents()
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("All agents retrieved", agents));
    }

    // ─── GET /agents/count/available ─────────────────────────────────────────

    @GetMapping("/count/available")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countAvailable() {
        log.info("GET /agents/count/available");
        long count = deliveryService.countAvailableAgents();
        return ResponseEntity.ok(ApiResponse.success(
                "Available agents count", Map.of("availableAgents", count)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mappers
    // ─────────────────────────────────────────────────────────────────────────

    private DeliveryAgentResponse toResponse(DeliveryAgent a) {
        return new DeliveryAgentResponse(
                                a.getAgentId(), a.getUserId(), a.getRestaurantId(), a.getFullName(), a.getPhone(),
                a.getVehicleType(), a.getVehicleNumber(),
                a.getCurrentLatitude(), a.getCurrentLongitude(),
                a.getIsAvailable(), a.getIsVerified(),
                a.getAvgRating(), a.getTotalDeliveries(),
                a.getCurrentOrderId(), a.getCreatedAt(), a.getLocationUpdatedAt()
        );
    }

    private NearbyAgentResponse toNearbyResponse(DeliveryAgent a, Double queryLat, Double queryLon) {
        double distKm = (a.getCurrentLatitude() != null && a.getCurrentLongitude() != null)
                ? DeliveryServiceImpl.haversineDistanceKm(
                        queryLat, queryLon,
                        a.getCurrentLatitude(), a.getCurrentLongitude())
                : -1.0;

        return new NearbyAgentResponse(
                a.getAgentId(), a.getFullName(), a.getPhone(),
                a.getVehicleType(),
                a.getCurrentLatitude(), a.getCurrentLongitude(),
                a.getAvgRating(), a.getTotalDeliveries(),
                distKm
        );
    }
}
