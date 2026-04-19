package com.fooddelivery.cartservice.service;

import com.fooddelivery.cartservice.entity.Cart;

import java.util.List;

/**
 * CartService — declares all cart retrieval, item management,
 * promo code application, and restaurant-switch operations.
 */
public interface CartService {

    /**
     * Retrieve the active cart for a given customer.
     * Creates a new empty cart if none exists.
     */
    Cart getCartByCustomerId(String customerId);

    /**
     * Add an item to the customer's cart.
     * Validates that the item belongs to the cart's current restaurant.
     * If the customer has no cart yet, creates one.
     * If the customer switches restaurants, throws RestaurantMismatchException.
     */
    Cart addItem(String customerId, Long restaurantId, Long menuItemId,
                 String name, Double price, Integer quantity, String customization);

    /**
     * Remove a specific item from the cart by menuItemId.
     */
    Cart removeItem(String customerId, Long menuItemId);

    /**
     * Clear all items from the customer's cart.
     */
    void clearCart(String customerId);

    /**
     * Compute and return the current cart total.
     */
    Double cartTotal(String customerId);

    /**
     * Change the restaurant associated with the cart.
     * This clears all existing items (enforces single-restaurant ordering).
     */
    Cart changeRestaurant(String customerId, Long restaurantId);

    /**
     * Apply a promo code to the cart and return the updated total.
     */
    Cart applyPromoCode(String customerId, String promoCode);

    /**
     * Get all carts (admin use).
     */
    List<Cart> getAllCarts();

    /**
     * Update the quantity of a specific item in the cart.
     */
    Cart updateQuantity(String customerId, Long menuItemId, Integer quantity);
}
