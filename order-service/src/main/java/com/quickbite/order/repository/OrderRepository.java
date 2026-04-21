package com.quickbite.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quickbite.order.entity.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** All orders for a customer, newest first */
       List<Order> findByCustomerIdOrderByOrderDateDesc(String customerId);

    /** All orders for a restaurant */
    List<Order> findByRestaurantIdOrderByOrderDateDesc(Long restaurantId);

    /** All orders with a given status (e.g. PLACED, PREPARING) */
    List<Order> findByOrderStatusOrderByOrderDateDesc(String orderStatus);

    /** All orders assigned to a delivery agent */
    List<Order> findByDeliveryAgentIdOrderByOrderDateDesc(Long deliveryAgentId);

    /** Find a single order by its ID */
    Optional<Order> findByOrderId(Long orderId);

    /** Orders placed within a date range (analytics / reports) */
    List<Order> findByOrderDateBetween(LocalDateTime from, LocalDateTime to);

    /** Total order count per restaurant */
    long countByRestaurantId(Long restaurantId);

    /** Active (non-terminal) orders for a customer */
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId " +
           "AND o.orderStatus NOT IN ('DELIVERED', 'CANCELLED') " +
           "ORDER BY o.orderDate DESC")
       List<Order> findActiveOrdersByCustomerId(@Param("customerId") String customerId);

    /** Active orders for a restaurant (not yet delivered or cancelled) */
    @Query("SELECT o FROM Order o WHERE o.restaurantId = :restaurantId " +
           "AND o.orderStatus NOT IN ('DELIVERED', 'CANCELLED') " +
           "ORDER BY o.orderDate ASC")
    List<Order> findActiveOrdersByRestaurantId(@Param("restaurantId") Long restaurantId);

    /** Count of all orders platform-wide */
    @Query("SELECT COUNT(o) FROM Order o")
    long countAllOrders();

    /** Orders by customer AND status */
       List<Order> findByCustomerIdAndOrderStatus(String customerId, String orderStatus);
}
