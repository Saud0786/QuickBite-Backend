package com.quickbite.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.dto.CartResponse;
import com.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.OrderItem;
import com.quickbite.order.exception.*;
import com.quickbite.order.feign.CartServiceClient;
import com.quickbite.order.feign.DeliveryServiceClient;
import com.quickbite.order.feign.RestaurantServiceClient;
import com.quickbite.order.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartServiceClient cartServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private final DeliveryServiceClient deliveryServiceClient;

    // ─── Allowed status transitions ───────────────────────────────────────────
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
        "PLACED",    Set.of("CONFIRMED", "CANCELLED"),
        "CONFIRMED", Set.of("PREPARING", "CANCELLED"),
        "PREPARING", Set.of("PACKING", "CANCELLED"),
        "PACKING",   Set.of("PICKED_UP", "CANCELLED"),
        "PICKED_UP", Set.of("DELIVERED"),
        "DELIVERED", Set.of(),
        "CANCELLED", Set.of()
    );

    // Estimated delivery offset in minutes
    private static final int ESTIMATED_DELIVERY_MINUTES = 45;

    // ─────────────────────────────────────────────────────────────────────────
    // placeOrder
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Order placeOrder(PlaceOrderRequest request) {
        log.debug("placeOrder: customerId={}, payment={}", request.getCustomerId(), request.getModeOfPayment());

        validatePaymentMode(request.getModeOfPayment());

        // 1. Fetch customer's cart via Feign
        CartResponse cartResponse = cartServiceClient.getCart(request.getCustomerId());
        CartResponse.CartData cart = cartResponse.getData();

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cannot place order: cart is empty for customer "
                    + request.getCustomerId());
        }

        // 2. Build Order entity
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setRestaurantId(cart.getRestaurantId());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setModeOfPayment(request.getModeOfPayment().toUpperCase());
        order.setSpecialInstructions(request.getSpecialInstructions());
        order.setOrderStatus("PLACED");
        order.setOrderDate(LocalDateTime.now());
        order.setEstimatedDelivery(LocalDateTime.now().plusMinutes(ESTIMATED_DELIVERY_MINUTES));
        order.setDiscount(0.0);

        // 3. Snapshot CartItems → OrderItems
        double total = 0.0;
        for (CartResponse.CartItemData cartItem : cart.getItems()) {
            OrderItem oi = new OrderItem(
                    cartItem.getMenuItemId(),
                    cartItem.getName(),
                    cartItem.getPrice(),
                    cartItem.getQuantity(),
                    cartItem.getCustomization()
            );
            order.addOrderItem(oi);
            total += cartItem.getPrice() * cartItem.getQuantity();
        }

        order.setTotalAmount(total);
        order.setFinalAmount(total - order.getDiscount());

        // 4. Persist order
        Order saved = orderRepository.save(order);
        log.info("Order placed: orderId={}, customerId={}, total={}",
                saved.getOrderId(), saved.getCustomerId(), saved.getFinalAmount());

        // 5. Clear cart after successful order placement
        try {
            cartServiceClient.clearCart(request.getCustomerId());
            log.debug("Cart cleared for customerId={}", request.getCustomerId());
        } catch (Exception e) {
            // Cart clear failure should not roll back the order — log and continue
            log.warn("Failed to clear cart for customerId={}: {}", request.getCustomerId(), e.getMessage());
        }

        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getOrderById
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        log.debug("getOrderById: {}", orderId);
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getOrdersByCustomer
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(String customerId) {
        log.debug("getOrdersByCustomer: {}", customerId);
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getOrdersByRestaurant
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByRestaurant(Long restaurantId) {
        log.debug("getOrdersByRestaurant: {}", restaurantId);
        return orderRepository.findByRestaurantIdOrderByOrderDateDesc(restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByRestaurantForOwner(Long restaurantId, String ownerId) {
        assertOwnerOwnsRestaurant(restaurantId, ownerId);
        return getOrdersByRestaurant(restaurantId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getActiveOrders  (restaurant's live order queue)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Order> getActiveOrders(Long restaurantId) {
        log.debug("getActiveOrders: restaurantId={}", restaurantId);
        return orderRepository.findActiveOrdersByRestaurantId(restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getActiveOrdersForOwner(Long restaurantId, String ownerId) {
        assertOwnerOwnsRestaurant(restaurantId, ownerId);
        return getActiveOrders(restaurantId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateStatus
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Order updateStatus(Long orderId, String newStatus) {
        log.debug("updateStatus: orderId={}, newStatus={}", orderId, newStatus);

        Order order = getOrderById(orderId);
        String current = order.getOrderStatus();

        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(newStatus.toUpperCase())) {
            throw new InvalidStatusTransitionException(
                "Cannot transition order " + orderId + " from [" + current + "] to [" + newStatus + "]. " +
                "Allowed transitions: " + allowed);
        }

        order.setOrderStatus(newStatus.toUpperCase());
        Order updated = orderRepository.save(order);
        log.info("Order {} status updated: {} → {}", orderId, current, newStatus.toUpperCase());
        return updated;
    }

    @Override
    @Transactional
    public Order updateStatusByOwner(Long orderId, String newStatus, String ownerId) {
        Order order = getOrderById(orderId);
        assertOwnerOwnsRestaurant(order.getRestaurantId(), ownerId);
        return updateStatus(orderId, newStatus);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // assignDeliveryAgent
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Order assignDeliveryAgent(Long orderId, Long deliveryAgentId) {
        log.debug("assignDeliveryAgent: orderId={}, agentId={}", orderId, deliveryAgentId);

        Order order = getOrderById(orderId);

        String status = order.getOrderStatus();
        if (!"CONFIRMED".equals(status) && !"PREPARING".equals(status) && !"PACKING".equals(status)) {
            throw new InvalidStatusTransitionException(
                "Cannot assign delivery agent to order " + orderId +
                " with status [" + status + "]. Must be CONFIRMED, PREPARING, or PACKING.");
        }

        // Reserve the agent in delivery-service first; if this fails, order assignment is not persisted.
        deliveryServiceClient.assignOrder(deliveryAgentId, Map.of("orderId", orderId));

        order.setDeliveryAgentId(deliveryAgentId);
        Order updated = orderRepository.save(order);
        log.info("Agent {} assigned to order {}", deliveryAgentId, orderId);
        return updated;
    }

    @Override
    @Transactional
    public Order markOrderDelivered(Long orderId, Long deliveryAgentId) {
        log.debug("markOrderDelivered: orderId={}, agentId={}", orderId, deliveryAgentId);

        Order order = getOrderById(orderId);

        if (order.getDeliveryAgentId() == null) {
            throw new UnauthorizedActionException(
                    "Order " + orderId + " does not have an assigned delivery agent.");
        }

        if (!deliveryAgentId.equals(order.getDeliveryAgentId())) {
            throw new UnauthorizedActionException(
                    "Delivery agent " + deliveryAgentId + " is not assigned to order " + orderId + ".");
        }

        if (order.isDelivered()) {
            return order;
        }

        if ("CANCELLED".equals(order.getOrderStatus())) {
            throw new InvalidStatusTransitionException(
                    "Cannot mark cancelled order " + orderId + " as delivered.");
        }

        order.setOrderStatus("DELIVERED");
        Order updated = orderRepository.save(order);
        log.info("Order {} marked delivered by agent {}", orderId, deliveryAgentId);
        return updated;
    }

    @Override
    @Transactional
    public Order assignDeliveryAgentByOwner(Long orderId, Long deliveryAgentId, String ownerId) {
        Order order = getOrderById(orderId);
        assertOwnerOwnsRestaurant(order.getRestaurantId(), ownerId);
        return assignDeliveryAgent(orderId, deliveryAgentId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // cancelOrder
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        log.debug("cancelOrder: orderId={}", orderId);

        Order order = getOrderById(orderId);

        if (!order.isCancellable()) {
            throw new OrderCancellationException(
                "Order " + orderId + " cannot be cancelled. Current status: [" +
                order.getOrderStatus() + "]. Only PLACED or CONFIRMED orders can be cancelled.");
        }

        order.setOrderStatus("CANCELLED");
        orderRepository.save(order);
        log.info("Order {} cancelled successfully", orderId);

        // In a full implementation: trigger refund via payment-service Feign call here
    }

    @Override
    @Transactional
    public void cancelOrderByOwner(Long orderId, String ownerId) {
        Order order = getOrderById(orderId);
        assertOwnerOwnsRestaurant(order.getRestaurantId(), ownerId);
        cancelOrder(orderId);
    }

    @Override
    @Transactional
    public void cancelOrderByCustomer(Long orderId, String customerId) {
        Order order = getOrderById(orderId);
        if (!customerId.equals(order.getCustomerId())) {
            throw new UnauthorizedActionException("You can only cancel your own orders.");
        }
        if (!"PLACED".equals(order.getOrderStatus())) {
            throw new OrderCancellationException(
                "You can cancel only while order is in PLACED status. Current status: ["
                    + order.getOrderStatus() + "]."
            );
        }
        cancelOrder(orderId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reorderFromHistory
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Order reorderFromHistory(Long orderId, PlaceOrderRequest request) {
        log.debug("reorderFromHistory: sourceOrderId={}, customerId={}", orderId, request.getCustomerId());

        Order source = getOrderById(orderId);

        // Validate the reorder belongs to the same customer
        if (!source.getCustomerId().equals(request.getCustomerId())) {
            throw new IllegalArgumentException("Order " + orderId + " does not belong to customer "
                    + request.getCustomerId());
        }

        validatePaymentMode(request.getModeOfPayment());

        // Build a fresh order from the historical order's items
        Order newOrder = new Order();
        newOrder.setCustomerId(request.getCustomerId());
        newOrder.setRestaurantId(source.getRestaurantId());
        newOrder.setDeliveryAddress(request.getDeliveryAddress());
        newOrder.setModeOfPayment(request.getModeOfPayment().toUpperCase());
        newOrder.setSpecialInstructions(request.getSpecialInstructions());
        newOrder.setOrderStatus("PLACED");
        newOrder.setOrderDate(LocalDateTime.now());
        newOrder.setEstimatedDelivery(LocalDateTime.now().plusMinutes(ESTIMATED_DELIVERY_MINUTES));
        newOrder.setDiscount(0.0);

        double total = 0.0;
        for (OrderItem src : source.getOrderItems()) {
            OrderItem item = new OrderItem(
                    src.getMenuItemId(),
                    src.getName(),
                    src.getPrice(),
                    src.getQuantity(),
                    src.getCustomization()
            );
            newOrder.addOrderItem(item);
            total += src.getPrice() * src.getQuantity();
        }

        newOrder.setTotalAmount(total);
        newOrder.setFinalAmount(total);

        Order saved = orderRepository.save(newOrder);
        log.info("Reorder created: newOrderId={}, from sourceOrderId={}", saved.getOrderId(), orderId);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getOrderCount
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public int getOrderCount() {
        return (int) orderRepository.countAllOrders();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void validatePaymentMode(String mode) {
        Set<String> valid = Set.of("COD", "CARD", "UPI", "WALLET");
        if (!valid.contains(mode.toUpperCase())) {
            throw new IllegalArgumentException(
                "Invalid payment mode: " + mode + ". Accepted: " + valid);
        }
    }

    private void assertOwnerOwnsRestaurant(Long restaurantId, String ownerId) {
        ApiResponse<com.quickbite.order.dto.RestaurantResponse> response =
                restaurantServiceClient.getRestaurantById(restaurantId);

        com.quickbite.order.dto.RestaurantResponse restaurant = response != null ? response.getData() : null;
        if (restaurant == null || restaurant.getOwnerId() == null) {
            throw new UnauthorizedActionException("Unable to verify restaurant ownership.");
        }

        if (!ownerId.equals(restaurant.getOwnerId())) {
            throw new UnauthorizedActionException("You are not allowed to manage orders for this restaurant.");
        }
    }
}
