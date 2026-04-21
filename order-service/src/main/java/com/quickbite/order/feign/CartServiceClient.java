package com.quickbite.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.order.dto.CartResponse;

/**
 * Feign client for cart-service.
 * Registered in Eureka as "cart-service".
 */
@FeignClient(name = "cart-service")
public interface CartServiceClient {

    /**
     * Fetch the customer's cart to snapshot items into an Order.
     */
    @GetMapping("/api/cart/{customerId}")
    CartResponse getCart(@PathVariable("customerId") String customerId);

    /**
     * Clear the cart after a successful order placement.
     */
    @DeleteMapping("/api/cart/{customerId}")
    void clearCart(@PathVariable("customerId") String customerId);
}
