package com.quickbite.delivery.service;

import com.quickbite.delivery.dto.RegisterAgentRequest;
import com.quickbite.delivery.entity.DeliveryAgent;
import com.quickbite.delivery.feign.OrderServiceClient;
import com.quickbite.delivery.exception.*;
import com.quickbite.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderServiceClient orderServiceClient;

    @Value("${delivery.nearby-radius-km:10.0}")
    private Double defaultRadiusKm;

    private static final Set<String> VALID_VEHICLE_TYPES =
            Set.of("BIKE", "BICYCLE", "SCOOTER", "CAR");

    // ─────────────────────────────────────────────────────────────────────────
    // registerAgent
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DeliveryAgent registerAgent(RegisterAgentRequest request) {
        log.debug("registerAgent: userId={}, phone={}", request.getUserId(), request.getPhone());

        if (deliveryRepository.existsByUserId(request.getUserId())) {
            throw new AgentAlreadyExistsException(
                    "A delivery agent is already registered for userId: " + request.getUserId());
        }

        if (deliveryRepository.existsByPhone(request.getPhone())) {
            throw new AgentAlreadyExistsException(
                    "Phone number already registered: " + request.getPhone());
        }

        String vehicleType = request.getVehicleType().toUpperCase();
        if (!VALID_VEHICLE_TYPES.contains(vehicleType)) {
            throw new IllegalArgumentException(
                    "Invalid vehicleType: " + request.getVehicleType()
                    + ". Valid types: " + VALID_VEHICLE_TYPES);
        }

        DeliveryAgent agent = new DeliveryAgent();
        agent.setUserId(request.getUserId());
        agent.setRestaurantId(request.getRestaurantId());
        agent.setFullName(request.getFullName());
        agent.setPhone(request.getPhone());
        agent.setVehicleType(vehicleType);
        agent.setVehicleNumber(request.getVehicleNumber().toUpperCase());
        agent.setIsAvailable(false);    // offline by default
        agent.setIsVerified(false);     // awaiting admin verification
        agent.setAvgRating(0.0);
        agent.setTotalDeliveries(0);
        agent.setCreatedAt(LocalDateTime.now());

        DeliveryAgent saved = deliveryRepository.save(agent);
        log.info("Agent registered: agentId={}, userId={} — awaiting admin verification",
                saved.getAgentId(), saved.getUserId());
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAgentById
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgent getAgentById(Long agentId) {
        log.debug("getAgentById: {}", agentId);
        return deliveryRepository.findByAgentId(agentId)
                .orElseThrow(() -> new AgentNotFoundException(
                        "Delivery agent not found: agentId=" + agentId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAgentByUserId
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgent getAgentByUserId(Long userId) {
        log.debug("getAgentByUserId: {}", userId);
        return deliveryRepository.findByUserId(userId)
                .orElseThrow(() -> new AgentNotFoundException(
                        "Delivery agent not found for userId=" + userId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAgentsByRestaurantId
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgent> getAgentsByRestaurantId(Long restaurantId) {
        log.debug("getAgentsByRestaurantId: restaurantId={}", restaurantId);
        return deliveryRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getNearbyAgents  (Haversine via JPQL)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgent> getNearbyAgents(Double latitude, Double longitude, Double radiusKm) {
        double radius = (radiusKm != null && radiusKm > 0) ? radiusKm : defaultRadiusKm;
        log.debug("getNearbyAgents: lat={}, lon={}, radius={}km", latitude, longitude, radius);

        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("latitude and longitude are required");
        }
        validateCoordinates(latitude, longitude);

        List<DeliveryAgent> agents =
                deliveryRepository.findNearbyAvailableAgents(latitude, longitude, radius);
        log.debug("Found {} nearby agents within {}km", agents.size(), radius);
        return agents;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateLocation
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DeliveryAgent updateLocation(Long agentId, Double latitude, Double longitude) {
        log.debug("updateLocation: agentId={}, lat={}, lon={}", agentId, latitude, longitude);

        validateCoordinates(latitude, longitude);
        DeliveryAgent agent = getAgentById(agentId);

        if (!Boolean.TRUE.equals(agent.getIsVerified())) {
            throw new AgentNotVerifiedException(
                    "Agent " + agentId + " is not verified. Location update not allowed.");
        }

        agent.setCurrentLatitude(latitude);
        agent.setCurrentLongitude(longitude);
        agent.setLocationUpdatedAt(LocalDateTime.now());

        DeliveryAgent saved = deliveryRepository.save(agent);
        log.debug("Location updated for agentId={}: ({}, {})", agentId, latitude, longitude);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // setAvailability
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DeliveryAgent setAvailability(Long agentId, Boolean available) {
        log.debug("setAvailability: agentId={}, available={}", agentId, available);

        DeliveryAgent agent = getAgentById(agentId);

        // Only verified agents can go online
        if (Boolean.TRUE.equals(available) && !Boolean.TRUE.equals(agent.getIsVerified())) {
            throw new AgentNotVerifiedException(
                    "Agent " + agentId + " must be verified before going online.");
        }

        // Cannot go offline while on an active delivery
        if (Boolean.FALSE.equals(available) && agent.getCurrentOrderId() != null) {
            throw new AgentUnavailableException(
                    "Agent " + agentId + " is currently on delivery for orderId="
                    + agent.getCurrentOrderId() + ". Complete the delivery first.");
        }

        agent.setIsAvailable(available);
        DeliveryAgent saved = deliveryRepository.save(agent);
        log.info("Agent {} availability set to {}", agentId, available);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // verifyAgent  (admin action)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DeliveryAgent verifyAgent(Long agentId) {
        log.debug("verifyAgent: agentId={}", agentId);

        DeliveryAgent agent = getAgentById(agentId);

        if (Boolean.TRUE.equals(agent.getIsVerified())) {
            log.warn("Agent {} is already verified", agentId);
            return agent;
        }

        agent.setIsVerified(true);
        DeliveryAgent saved = deliveryRepository.save(agent);
        log.info("Agent {} verified by admin", agentId);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // assignOrder
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DeliveryAgent assignOrder(Long agentId, Long orderId) {
        log.debug("assignOrder: agentId={}, orderId={}", agentId, orderId);

        DeliveryAgent agent = getAgentById(agentId);

        if (!Boolean.TRUE.equals(agent.getIsVerified())) {
            throw new AgentNotVerifiedException(
                    "Agent " + agentId + " is not verified and cannot be assigned orders.");
        }

        if (!Boolean.TRUE.equals(agent.getIsAvailable())) {
            throw new AgentUnavailableException(
                    "Agent " + agentId + " is currently offline.");
        }

        if (agent.getCurrentOrderId() != null) {
            throw new AgentUnavailableException(
                    "Agent " + agentId + " is already on delivery for orderId="
                    + agent.getCurrentOrderId());
        }

        agent.setCurrentOrderId(orderId);
        agent.setIsAvailable(false);   // mark busy during delivery

        DeliveryAgent saved = deliveryRepository.save(agent);
        log.info("Order {} assigned to agent {}", orderId, agentId);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // completeDelivery
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DeliveryAgent completeDelivery(Long agentId) {
        log.debug("completeDelivery: agentId={}", agentId);

        DeliveryAgent agent = getAgentById(agentId);

        if (agent.getCurrentOrderId() == null) {
            throw new IllegalArgumentException(
                    "Agent " + agentId + " has no active delivery to complete.");
        }

        Long completedOrderId = agent.getCurrentOrderId();

    orderServiceClient.markDelivered(completedOrderId, Map.of("deliveryAgentId", agentId));

        agent.setCurrentOrderId(null);
        agent.setIsAvailable(true);                              // back online after delivery
        agent.setTotalDeliveries(agent.getTotalDeliveries() + 1);

        DeliveryAgent saved = deliveryRepository.save(agent);
        log.info("Agent {} completed delivery for orderId={}. Total deliveries={}",
                agentId, completedOrderId, saved.getTotalDeliveries());
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateRating  (rolling average)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DeliveryAgent updateRating(Long agentId, Double newRating) {
        log.debug("updateRating: agentId={}, newRating={}", agentId, newRating);

        if (newRating < 1.0 || newRating > 5.0) {
            throw new IllegalArgumentException("Rating must be between 1.0 and 5.0");
        }

        DeliveryAgent agent = getAgentById(agentId);

        int total = agent.getTotalDeliveries();
        double currentAvg = agent.getAvgRating();

        /*
         * Rolling average formula:
         *   newAvg = ((currentAvg * totalDeliveries) + newRating) / (totalDeliveries + 1)
         *
         * We use totalDeliveries as the count of existing ratings (one rating per delivery).
         * If no deliveries yet, the new rating becomes the average directly.
         */
        double updatedAvg;
        if (total == 0) {
            updatedAvg = newRating;
        } else {
            updatedAvg = ((currentAvg * total) + newRating) / (total + 1);
        }

        // Round to 2 decimal places
        updatedAvg = Math.round(updatedAvg * 100.0) / 100.0;
        agent.setAvgRating(updatedAvg);

        DeliveryAgent saved = deliveryRepository.save(agent);
        log.info("Agent {} rating updated: {} → {} (after {} deliveries)",
                agentId, currentAvg, updatedAvg, total);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getActiveDeliveries
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgent> getActiveDeliveries() {
        log.debug("getActiveDeliveries");
        return deliveryRepository.findByCurrentOrderIdIsNotNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllAgents
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgent> getAllAgents() {
        log.debug("getAllAgents");
        return deliveryRepository.findAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // countAvailableAgents
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public long countAvailableAgents() {
        return deliveryRepository.countByIsAvailable(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void validateCoordinates(Double lat, Double lon) {
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90. Got: " + lat);
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180. Got: " + lon);
        }
    }

    /**
     * Haversine distance calculation in Java — used by the service layer
     * when computing distances for the NearbyAgentResponse DTO.
     */
    public static double haversineDistanceKm(double lat1, double lon1,
                                              double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 100.0) / 100.0;
    }
}
