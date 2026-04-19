package com.fooddelivery.cartservice.service;

import com.fooddelivery.cartservice.entity.Cart;
import com.fooddelivery.cartservice.entity.CartItem;
import com.fooddelivery.cartservice.exception.CartNotFoundException;
import com.fooddelivery.cartservice.exception.ItemNotFoundException;
import com.fooddelivery.cartservice.exception.RestaurantMismatchException;
import com.fooddelivery.cartservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;

    // Promo code discount percentage (in a real system, fetch from a promo-service)
    private static final double PROMO_DISCOUNT = 0.10; // 10% discount

    // ─────────────────────────────────────────────────────────────────────────────
    // getCartByCustomerId
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Cart getCartByCustomerId(String customerId) {
        log.debug("Fetching cart for customerId={}", customerId);
        return cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(
                        "No active cart found for customer: " + customerId));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // addItem
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Cart addItem(String customerId, Long restaurantId, Long menuItemId,
                        String name, Double price, Integer quantity, String customization) {

        log.debug("addItem: customerId={}, restaurantId={}, menuItemId={}", customerId, restaurantId, menuItemId);

        Cart cart = cartRepository.findByCustomerId(customerId).orElseGet(() -> {
            log.debug("No cart found for customer {}. Creating a new cart.", customerId);
            Cart newCart = new Cart();
            newCart.setCustomerId(customerId);
            newCart.setRestaurantId(restaurantId);
            newCart.setTotalPrice(0.0);
            return cartRepository.save(newCart);
        });

        // Enforce single-restaurant ordering
        if (!cart.getRestaurantId().equals(restaurantId)) {
            log.warn("Restaurant mismatch: cart has restaurantId={}, request has restaurantId={}",
                    cart.getRestaurantId(), restaurantId);
            throw new RestaurantMismatchException(
                    "Your cart already has items from a different restaurant (ID: " + cart.getRestaurantId() +
                    "). Clear your cart before ordering from a new restaurant.");
        }

        // Check if item already exists in cart — if so, increment quantity
        CartItem existingItem = cart.getCartItemByMenuItemId(menuItemId);
        if (existingItem != null) {
            log.debug("Item menuItemId={} already in cart. Incrementing quantity.", menuItemId);
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            CartItem newItem = new CartItem(menuItemId, name, price, quantity, customization);
            cart.addCartItem(newItem);
        }

        cart.recalculateTotal();
        return cartRepository.save(cart);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // removeItem
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Cart removeItem(String customerId, Long menuItemId) {
        log.debug("removeItem: customerId={}, menuItemId={}", customerId, menuItemId);

        Cart cart = getCartByCustomerId(customerId);

        CartItem itemToRemove = cart.getCartItemByMenuItemId(menuItemId);
        if (itemToRemove == null) {
            throw new ItemNotFoundException("Item with menuItemId=" + menuItemId + " not found in cart.");
        }

        cart.removeCartItem(itemToRemove);
        cart.recalculateTotal();
        return cartRepository.save(cart);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // clearCart
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void clearCart(String customerId) {
        log.debug("clearCart: customerId={}", customerId);

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(
                        "No cart to clear for customer: " + customerId));

        cart.clearCart();
        cartRepository.save(cart);
        log.info("Cart cleared for customerId={}", customerId);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // cartTotal
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Double cartTotal(String customerId) {
        log.debug("cartTotal: customerId={}", customerId);
        Cart cart = getCartByCustomerId(customerId);
        return cart.getTotalPrice();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // changeRestaurant
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Cart changeRestaurant(String customerId, Long restaurantId) {
        log.debug("changeRestaurant: customerId={}, new restaurantId={}", customerId, restaurantId);

        Cart cart = cartRepository.findByCustomerId(customerId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setCustomerId(customerId);
            newCart.setRestaurantId(restaurantId);
            newCart.setTotalPrice(0.0);
            return cartRepository.save(newCart);
        });

        // Clear existing items and switch restaurant
        cart.clearCart();
        cart.setRestaurantId(restaurantId);
        log.info("Cart for customerId={} switched to restaurantId={}. Items cleared.", customerId, restaurantId);
        return cartRepository.save(cart);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // applyPromoCode
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Cart applyPromoCode(String customerId, String promoCode) {
        log.debug("applyPromoCode: customerId={}, promoCode={}", customerId, promoCode);

        Cart cart = getCartByCustomerId(customerId);

        // In a real system, call a promo-service via Feign to validate and get discount
        // Here we apply a hardcoded discount for demo purposes
        if (isValidPromoCode(promoCode)) {
            double originalTotal = cart.getTotalPrice();
            double discountedTotal = originalTotal * (1 - PROMO_DISCOUNT);
            cart.setTotalPrice(Math.round(discountedTotal * 100.0) / 100.0);
            log.info("Promo code '{}' applied. Original: {}, Discounted: {}",
                    promoCode, originalTotal, cart.getTotalPrice());
        } else {
            throw new IllegalArgumentException("Invalid or expired promo code: " + promoCode);
        }

        return cartRepository.save(cart);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // getAllCarts (admin)
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Cart> getAllCarts() {
        log.debug("getAllCarts: fetching all carts");
        return cartRepository.findAll();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // updateQuantity
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Cart updateQuantity(String customerId, Long menuItemId, Integer quantity) {
        log.debug("updateQuantity: customerId={}, menuItemId={}, qty={}", customerId, menuItemId, quantity);

        Cart cart = getCartByCustomerId(customerId);

        CartItem item = cart.getCartItemByMenuItemId(menuItemId);
        if (item == null) {
            throw new ItemNotFoundException("Item with menuItemId=" + menuItemId + " not found in cart.");
        }

        if (quantity <= 0) {
            // If quantity is 0 or negative, remove the item
            cart.removeCartItem(item);
        } else {
            item.setQuantity(quantity);
        }

        cart.recalculateTotal();
        return cartRepository.save(cart);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Basic promo code validation.
     * In production, call promo-service via Feign client.
     */
    private boolean isValidPromoCode(String promoCode) {
        // Accepted promo codes for demo
        return "SAVE10".equalsIgnoreCase(promoCode)
                || "WELCOME".equalsIgnoreCase(promoCode)
                || "FIRST10".equalsIgnoreCase(promoCode);
    }
}
