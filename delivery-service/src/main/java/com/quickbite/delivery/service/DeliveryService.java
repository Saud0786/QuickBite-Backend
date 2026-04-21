package com.quickbite.delivery.service;

import com.quickbite.delivery.dto.RegisterAgentRequest;
import com.quickbite.delivery.entity.DeliveryAgent;

import java.util.List;

/**
 * DeliveryService — declares agent registration, geo-proximity lookup,
 * location update, availability toggle, verification, assignment, and rating operations.
 */
public interface DeliveryService {

    /**
     * Register a new delivery agent. Awaits admin verification before activation.
     */
    DeliveryAgent registerAgent(RegisterAgentRequest request);

    /**
     * Fetch an agent by their agentId.
     */
    DeliveryAgent getAgentById(Long agentId);

    /**
     * Fetch an agent by their auth-service userId.
     */
    DeliveryAgent getAgentByUserId(Long userId);

    /**
     * All agents registered for a specific restaurant.
     */
    List<DeliveryAgent> getAgentsByRestaurantId(Long restaurantId);

    /**
     * Find all available + verified agents within radiusKm of the given coordinates.
     * Uses the Haversine formula.
     */
    List<DeliveryAgent> getNearbyAgents(Double latitude, Double longitude, Double radiusKm);

    /**
     * Update the agent's live GPS coordinates.
     * Only allowed for verified agents.
     */
    DeliveryAgent updateLocation(Long agentId, Double latitude, Double longitude);

    /**
     * Toggle the agent's online/offline availability.
     * Only verified agents can go online.
     */
    DeliveryAgent setAvailability(Long agentId, Boolean available);

    /**
     * Admin verifies an agent after checking identity and vehicle documents.
     */
    DeliveryAgent verifyAgent(Long agentId);

    /**
     * Assign an order to a specific agent.
     * Agent must be verified, available, and not currently on another delivery.
     */
    DeliveryAgent assignOrder(Long agentId, Long orderId);

    /**
     * Mark the current delivery as complete.
     * Increments totalDeliveries, clears currentOrderId, and restores availability.
     */
    DeliveryAgent completeDelivery(Long agentId);

    /**
     * Update the agent's average rating using a rolling average formula.
     */
    DeliveryAgent updateRating(Long agentId, Double newRating);

    /**
     * All agents currently on an active delivery (for admin tracking).
     */
    List<DeliveryAgent> getActiveDeliveries();

    /**
     * All registered agents (admin use).
     */
    List<DeliveryAgent> getAllAgents();

    /**
     * Count of currently available + verified agents.
     */
    long countAvailableAgents();
}
