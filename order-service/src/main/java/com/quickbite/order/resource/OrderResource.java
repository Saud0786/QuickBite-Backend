package com.quickbite.order.resource;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.quickbite.order.dto.*;
import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.OrderItem;
import com.quickbite.order.exception.UnauthorizedActionException;
import com.quickbite.order.feign.AuthServiceClient;
import com.quickbite.order.security.JwtAuthService;
import com.quickbite.order.service.OrderService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * OrderResource — REST controller exposing /orders endpoints.
 *
 * POST   /orders/place                          → placeOrder
 * GET    /orders/{orderId}                      → getOrderById
 * GET    /orders/customer/{customerId}          → getOrdersByCustomer
 * GET    /orders/restaurant/{restaurantId}      → getOrdersByRestaurant
 * GET    /orders/restaurant/{restaurantId}/active → getActiveOrders
 * PUT    /orders/{orderId}/status               → updateStatus
 * PUT    /orders/{orderId}/assign-agent         → assignDeliveryAgent
 * PUT    /orders/{orderId}/delivery-complete    → markOrderDelivered (internal)
 * PUT    /orders/{orderId}/cancel               → cancelOrder
 * POST   /orders/{orderId}/reorder              → reorderFromHistory
 * GET    /orders/count                          → getOrderCount
 */
@RestController
@RequestMapping({"/orders", "/api/orders"})
@RequiredArgsConstructor
@Slf4j
public class OrderResource {

    private final OrderService orderService;
        private final JwtAuthService jwtAuthService;
        private final AuthServiceClient authServiceClient;

    // ─── POST /orders/place ───────────────────────────────────────────────────

    @PostMapping("/place")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {
        log.info("POST /orders/place — customerId={}", request.getCustomerId());
        Order order = orderService.placeOrder(request);
        Map<String, UserProfileResponse> profiles = fetchCustomerProfiles(Set.of(order.getCustomerId()));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", toResponse(order, profiles)));
    }

    // ─── GET /orders/{orderId} ────────────────────────────────────────────────

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long orderId) {
        log.info("GET /orders/{}", orderId);
        Order order = orderService.getOrderById(orderId);
                Map<String, UserProfileResponse> profiles = fetchCustomerProfiles(Set.of(order.getCustomerId()));
                return ResponseEntity.ok(ApiResponse.success("Order retrieved", toResponse(order, profiles)));
    }

    // ─── GET /orders/customer/{customerId} ───────────────────────────────────

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getByCustomer(
                        @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String customerId) {
        String requesterId = jwtAuthService.extractUserIdFromAuthorizationHeader(authorizationHeader);
        String role = jwtAuthService.extractRoleFromAuthorizationHeader(authorizationHeader);
        if (!customerId.equals(requesterId) && !"ADMIN".equals(role)) {
            throw new UnauthorizedActionException("You can only view your own order history.");
        }
        log.info("GET /orders/customer/{}", customerId);
        List<Order> orders = orderService.getOrdersByCustomer(customerId);
        Map<String, UserProfileResponse> profiles = fetchCustomerProfiles(
                orders.stream().map(Order::getCustomerId).collect(Collectors.toSet()),
                authorizationHeader
        );
        List<OrderResponse> responses = orders.stream().map(order -> toResponse(order, profiles)).toList();
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", responses));
    }

    // ─── GET /orders/restaurant/{restaurantId} ────────────────────────────────

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getByRestaurant(
                        @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long restaurantId) {
        jwtAuthService.assertOwnerRole(authorizationHeader);
        String role = jwtAuthService.extractRoleFromAuthorizationHeader(authorizationHeader);
        String requesterId = jwtAuthService.extractUserIdFromAuthorizationHeader(authorizationHeader);
        log.info("GET /orders/restaurant/{}", restaurantId);
        List<Order> orders = "ADMIN".equals(role)
                ? orderService.getOrdersByRestaurant(restaurantId)
                : orderService.getOrdersByRestaurantForOwner(restaurantId, requesterId);
        Map<String, UserProfileResponse> profiles = fetchCustomerProfiles(
                orders.stream().map(Order::getCustomerId).collect(Collectors.toSet()),
                authorizationHeader
        );
        List<OrderResponse> responses = orders.stream().map(order -> toResponse(order, profiles)).toList();
        return ResponseEntity.ok(ApiResponse.success("Restaurant orders retrieved", responses));
    }

    // ─── GET /orders/restaurant/{restaurantId}/active ─────────────────────────

    @GetMapping("/restaurant/{restaurantId}/active")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getActiveOrders(
                        @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long restaurantId) {
        jwtAuthService.assertOwnerRole(authorizationHeader);
        String role = jwtAuthService.extractRoleFromAuthorizationHeader(authorizationHeader);
        String requesterId = jwtAuthService.extractUserIdFromAuthorizationHeader(authorizationHeader);
        log.info("GET /orders/restaurant/{}/active", restaurantId);
        List<Order> orders = "ADMIN".equals(role)
                ? orderService.getActiveOrders(restaurantId)
                : orderService.getActiveOrdersForOwner(restaurantId, requesterId);
        Map<String, UserProfileResponse> profiles = fetchCustomerProfiles(
                orders.stream().map(Order::getCustomerId).collect(Collectors.toSet()),
                authorizationHeader
        );
        List<OrderResponse> responses = orders.stream().map(order -> toResponse(order, profiles)).toList();
        return ResponseEntity.ok(ApiResponse.success("Active orders retrieved", responses));
    }

    // ─── PUT /orders/{orderId}/status ─────────────────────────────────────────

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
                        @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateStatusRequest request) {
        jwtAuthService.assertOwnerRole(authorizationHeader);
        String role = jwtAuthService.extractRoleFromAuthorizationHeader(authorizationHeader);
        String requesterId = jwtAuthService.extractUserIdFromAuthorizationHeader(authorizationHeader);
        log.info("PUT /orders/{}/status — newStatus={}", orderId, request.getStatus());
        Order order = "ADMIN".equals(role)
                ? orderService.updateStatus(orderId, request.getStatus())
                : orderService.updateStatusByOwner(orderId, request.getStatus(), requesterId);
        Map<String, UserProfileResponse> profiles = fetchCustomerProfiles(Set.of(order.getCustomerId()), authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(
                "Order status updated to " + order.getOrderStatus(), toResponse(order, profiles)));
    }

    // ─── PUT /orders/{orderId}/assign-agent ───────────────────────────────────

    @PutMapping("/{orderId}/assign-agent")
    public ResponseEntity<ApiResponse<OrderResponse>> assignAgent(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long orderId,
            @Valid @RequestBody AssignAgentRequest request) {
        jwtAuthService.assertOwnerRole(authorizationHeader);
        String role = jwtAuthService.extractRoleFromAuthorizationHeader(authorizationHeader);
        String requesterId = jwtAuthService.extractUserIdFromAuthorizationHeader(authorizationHeader);
        log.info("PUT /orders/{}/assign-agent — agentId={}", orderId, request.getDeliveryAgentId());
        Order order = "ADMIN".equals(role)
                ? orderService.assignDeliveryAgent(orderId, request.getDeliveryAgentId())
                : orderService.assignDeliveryAgentByOwner(orderId, request.getDeliveryAgentId(), requesterId);
        Map<String, UserProfileResponse> profiles = fetchCustomerProfiles(Set.of(order.getCustomerId()));
        return ResponseEntity.ok(ApiResponse.success(
                "Delivery agent assigned successfully", toResponse(order, profiles)));
    }

        // ─── PUT /orders/{orderId}/delivery-complete ─────────────────────────────

        @PutMapping("/{orderId}/delivery-complete")
        public ResponseEntity<ApiResponse<OrderResponse>> markDelivered(
                        @PathVariable Long orderId,
                        @RequestBody Map<String, Long> body) {
                Long deliveryAgentId = body.get("deliveryAgentId");
                if (deliveryAgentId == null) {
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error("Field 'deliveryAgentId' is required."));
                }

                log.info("PUT /orders/{}/delivery-complete — deliveryAgentId={}", orderId, deliveryAgentId);
                Order order = orderService.markOrderDelivered(orderId, deliveryAgentId);
                Map<String, UserProfileResponse> profiles = fetchCustomerProfiles(Set.of(order.getCustomerId()));
                return ResponseEntity.ok(ApiResponse.success(
                                "Order marked delivered successfully", toResponse(order, profiles)));
        }

    // ─── PUT /orders/{orderId}/cancel ─────────────────────────────────────────

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
                        @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long orderId) {
                String role = jwtAuthService.extractRoleFromAuthorizationHeader(authorizationHeader);
                String requesterId = jwtAuthService.extractUserIdFromAuthorizationHeader(authorizationHeader);

        log.info("PUT /orders/{}/cancel", orderId);
                if ("ADMIN".equals(role)) {
                        orderService.cancelOrder(orderId);
                } else if ("OWNER".equals(role)) {
                        orderService.cancelOrderByOwner(orderId, requesterId);
                } else if ("CUSTOMER".equals(role)) {
                        orderService.cancelOrderByCustomer(orderId, requesterId);
                } else {
                        throw new UnauthorizedActionException("You are not allowed to cancel this order.");
                }

        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", null));
    }

    // ─── POST /orders/{orderId}/reorder ───────────────────────────────────────

    @PostMapping("/{orderId}/reorder")
    public ResponseEntity<ApiResponse<OrderResponse>> reorder(
                        @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long orderId,
            @Valid @RequestBody PlaceOrderRequest request) {
                String role = jwtAuthService.extractRoleFromAuthorizationHeader(authorizationHeader);
                String requesterId = jwtAuthService.extractUserIdFromAuthorizationHeader(authorizationHeader);
                if (!"ADMIN".equals(role)) {
                        if (!"CUSTOMER".equals(role)) {
                                throw new UnauthorizedActionException("Only customers can reorder from order history.");
                        }
                        if (!requesterId.equals(request.getCustomerId())) {
                                throw new UnauthorizedActionException("You can only reorder using your own customer account.");
                        }
                }
        log.info("POST /orders/{}/reorder — customerId={}", orderId, request.getCustomerId());
        Order order = orderService.reorderFromHistory(orderId, request);
                Map<String, UserProfileResponse> profiles = fetchCustomerProfiles(Set.of(order.getCustomerId()), authorizationHeader);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Reorder placed successfully", toResponse(order, profiles)));
    }

    // ─── GET /orders/count ────────────────────────────────────────────────────

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getCount() {
        log.info("GET /orders/count");
        int count = orderService.getOrderCount();
        return ResponseEntity.ok(ApiResponse.success("Order count retrieved",
                Map.of("totalOrders", count)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mapper: Order → OrderResponse
    // ─────────────────────────────────────────────────────────────────────────

    private OrderResponse toResponse(Order order, Map<String, UserProfileResponse> profiles) {
        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        UserProfileResponse profile = profiles.get(order.getCustomerId());

        return new OrderResponse(
                order.getOrderId(),
                order.getCustomerId(),
                profile != null ? profile.getFullName() : null,
                profile != null ? profile.getPhone() : null,
                order.getRestaurantId(),
                order.getDeliveryAgentId(),
                order.getTotalAmount(),
                order.getDiscount(),
                order.getFinalAmount(),
                order.getModeOfPayment(),
                order.getOrderStatus(),
                order.getOrderDate(),
                order.getEstimatedDelivery(),
                order.getDeliveryAddress(),
                order.getSpecialInstructions(),
                items,
                items.size()
        );
    }

        private Map<String, UserProfileResponse> fetchCustomerProfiles(Set<String> customerIds) {
                return fetchCustomerProfiles(customerIds, null);
            }

            private Map<String, UserProfileResponse> fetchCustomerProfiles(Set<String> customerIds, String authorizationHeader) {
                Map<String, UserProfileResponse> profiles = new HashMap<>();
                for (String customerId : customerIds) {
                        if (customerId == null || customerId.isBlank()) {
                                continue;
                        }
                                if (authorizationHeader == null || authorizationHeader.isBlank()) {
                                        continue;
                                }
                        try {
                                        ApiResponse<UserProfileResponse> response = authServiceClient.getUserById(customerId, authorizationHeader);
                                if (response != null && response.getData() != null) {
                                        profiles.put(customerId, response.getData());
                                }
                        } catch (Exception ex) {
                                log.warn("Unable to fetch customer profile for customerId={}: {}", customerId, ex.getMessage());
                        }
                }
                return profiles;
        }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getOrderItemId(),
                item.getMenuItemId(),
                item.getName(),
                item.getPrice(),
                item.getQuantity(),
                item.getCustomization(),
                item.getSubtotal()
        );
    }
}
