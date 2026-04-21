package com.quickbite.order.service;

import java.util.List;

import com.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.order.entity.Order;

/**
 * OrderService — declares all order placement, retrieval, status management,
 * agent assignment, cancellation, and reorder operations.
 */
public interface OrderService {

    /**
     * Place a new order from the customer's active cart.
     * Snapshots cart items → OrderItems, computes total, clears the cart.
     */
    Order placeOrder(PlaceOrderRequest request);

    /**
     * Fetch a single order by its ID.
     */
    Order getOrderById(Long orderId);

    /**
     * All orders for a customer, newest first.
     */
    List<Order> getOrdersByCustomer(String customerId);

    /**
     * All orders for a restaurant, newest first.
     */
    List<Order> getOrdersByRestaurant(Long restaurantId);

    /**
     * Owner-scoped read: only returns orders if the owner owns that restaurant.
     */
    List<Order> getOrdersByRestaurantForOwner(Long restaurantId, String ownerId);

    /**
     * Active (non-terminal) orders for a restaurant — the live order queue.
     */
    List<Order> getActiveOrders(Long restaurantId);

    /**
     * Owner-scoped read of active orders for a specific restaurant.
     */
    List<Order> getActiveOrdersForOwner(Long restaurantId, String ownerId);

    /**
     * Transition an order to the next status.
     * Validates allowed transitions:
     *   PLACED → CONFIRMED → PREPARING → PICKED_UP → DELIVERED
     */
    Order updateStatus(Long orderId, String newStatus);

    /**
     * Owner-scoped status update.
     */
    Order updateStatusByOwner(Long orderId, String newStatus, String ownerId);

    /**
     * Assign a verified delivery agent to an order.
     * Only allowed when status is CONFIRMED or PREPARING.
     */
    Order assignDeliveryAgent(Long orderId, Long deliveryAgentId);

    /**
     * Mark an order as delivered after the assigned delivery agent completes it.
     */
    Order markOrderDelivered(Long orderId, Long deliveryAgentId);

    /**
     * Owner-scoped delivery agent assignment.
     */
    Order assignDeliveryAgentByOwner(Long orderId, Long deliveryAgentId, String ownerId);

    /**
     * Cancel an order.
     * Only allowed when status is PLACED or CONFIRMED.
     */
    void cancelOrder(Long orderId);

    /**
     * Owner-scoped cancellation.
     */
    void cancelOrderByOwner(Long orderId, String ownerId);

    /**
     * Customer-scoped cancellation.
     */
    void cancelOrderByCustomer(Long orderId, String customerId);

    /**
     * Reorder from history — copies items from a past order into a new Order.
     * The new order has status PLACED and the same restaurant / items.
     */
    Order reorderFromHistory(Long orderId, PlaceOrderRequest request);

    /**
     * Total order count across the platform (admin analytics).
     */
    int getOrderCount();
}
