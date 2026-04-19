package com.fooddelivery.cartservice.resource;

import com.fooddelivery.cartservice.dto.*;
import com.fooddelivery.cartservice.entity.Cart;
import com.fooddelivery.cartservice.entity.CartItem;
import com.fooddelivery.cartservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CartResource — REST controller exposing /cart endpoints.
 *
 * GET    /cart/{customerId}                    → getCartByCustomer
 * POST   /cart/{customerId}/items              → addItem
 * DELETE /cart/{customerId}/items/{menuItemId} → removeItem
 * DELETE /cart/{customerId}                    → clearCart
 * PUT    /cart/{customerId}/items              → updateQuantity
 * PUT    /cart/{customerId}/restaurant         → changeRestaurant
 * POST   /cart/{customerId}/promo              → applyPromo
 * GET    /cart/all                             → getAllCarts (admin)
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartResource {

    private final CartService cartService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /cart/{customerId}
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CartResponse>> getCartByCustomer(
            @PathVariable String customerId) {
        log.info("GET /cart/{}", customerId);
        Cart cart = cartService.getCartByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", toResponse(cart)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /cart/{customerId}/items  → addItem
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{customerId}/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @PathVariable String customerId,
            @Valid @RequestBody AddItemRequest request) {
        log.info("POST /cart/{}/items — menuItemId={}", customerId, request.getMenuItemId());
        Cart cart = cartService.addItem(
                customerId,
                request.getRestaurantId(),
                request.getMenuItemId(),
                request.getName(),
                request.getPrice(),
                request.getQuantity(),
                request.getCustomization()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added to cart", toResponse(cart)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /cart/{customerId}/items/{menuItemId}  → removeItem
    // ─────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/{customerId}/items/{menuItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable String customerId,
            @PathVariable Long menuItemId) {
        log.info("DELETE /cart/{}/items/{}", customerId, menuItemId);
        Cart cart = cartService.removeItem(customerId, menuItemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", toResponse(cart)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /cart/{customerId}  → clearCart
    // ─────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/{customerId}")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @PathVariable String customerId) {
        log.info("DELETE /cart/{} — clearing cart", customerId);
        cartService.clearCart(customerId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /cart/{customerId}/items  → updateQuantity
    // ─────────────────────────────────────────────────────────────────────────

    @PutMapping("/{customerId}/items")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @PathVariable String customerId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        log.info("PUT /cart/{}/items — menuItemId={}, qty={}", customerId,
                request.getMenuItemId(), request.getQuantity());
        Cart cart = cartService.updateQuantity(customerId, request.getMenuItemId(), request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Quantity updated", toResponse(cart)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /cart/{customerId}/restaurant  → changeRestaurant
    // ─────────────────────────────────────────────────────────────────────────

    @PutMapping("/{customerId}/restaurant")
    public ResponseEntity<ApiResponse<CartResponse>> changeRestaurant(
            @PathVariable String customerId,
            @Valid @RequestBody ChangeRestaurantRequest request) {
        log.info("PUT /cart/{}/restaurant — restaurantId={}", customerId, request.getRestaurantId());
        Cart cart = cartService.changeRestaurant(customerId, request.getRestaurantId());
        return ResponseEntity.ok(ApiResponse.success(
                "Restaurant changed. Cart cleared.", toResponse(cart)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /cart/{customerId}/promo  → applyPromoCode
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{customerId}/promo")
    public ResponseEntity<ApiResponse<CartResponse>> applyPromo(
            @PathVariable String customerId,
            @Valid @RequestBody ApplyPromoRequest request) {
        log.info("POST /cart/{}/promo — code={}", customerId, request.getPromoCode());
        Cart cart = cartService.applyPromoCode(customerId, request.getPromoCode());
        return ResponseEntity.ok(ApiResponse.success(
                "Promo code applied successfully", toResponse(cart)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /cart/all  → getAllCarts (admin)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CartResponse>>> getAllCarts() {
        log.info("GET /cart/all");
        List<CartResponse> carts = cartService.getAllCarts()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("All carts retrieved", carts));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /cart/{customerId}/total  → cartTotal
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{customerId}/total")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getCartTotal(
            @PathVariable String customerId) {
        log.info("GET /cart/{}/total", customerId);
        Double total = cartService.cartTotal(customerId);
        return ResponseEntity.ok(ApiResponse.success("Cart total calculated",
                Map.of("total", total)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mapper: Cart → CartResponse
    // ─────────────────────────────────────────────────────────────────────────

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getCartItems()
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        return new CartResponse(
                cart.getCartId(),
                cart.getCustomerId(),
                cart.getRestaurantId(),
                cart.getTotalPrice(),
                items,
                items.size()
        );
    }

    private CartItemResponse toItemResponse(CartItem item) {
        return new CartItemResponse(
                item.getItemId(),
                item.getMenuItemId(),
                item.getName(),
                item.getPrice(),
                item.getQuantity(),
                item.getCustomization(),
                item.getPrice() * item.getQuantity()
        );
    }
}
