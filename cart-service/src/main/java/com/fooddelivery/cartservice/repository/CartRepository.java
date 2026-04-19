package com.fooddelivery.cartservice.repository;

import com.fooddelivery.cartservice.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Find cart by customer ID (each customer has only one active cart)
     */
    Optional<Cart> findByCustomerId(String customerId);

    /**
     * Find cart by cart ID
     */
    Optional<Cart> findByCartId(Long cartId);

    /**
     * Check if a cart already exists for given customer and restaurant
     */
    Boolean existsByCustomerIdAndRestaurantId(String customerId, Long restaurantId);

    /**
     * Find all carts for a specific restaurant (admin / analytics use)
     */
    List<Cart> findByRestaurantId(Long restaurantId);

    /**
     * Delete cart by customer ID (cleanup on logout / account delete)
     */
    void deleteByCustomerId(String customerId);
}
