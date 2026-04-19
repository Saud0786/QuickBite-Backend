package com.fooddelivery.cartservice.service;

import com.fooddelivery.cartservice.entity.Cart;
import com.fooddelivery.cartservice.entity.CartItem;
import com.fooddelivery.cartservice.exception.CartNotFoundException;
import com.fooddelivery.cartservice.exception.ItemNotFoundException;
import com.fooddelivery.cartservice.exception.RestaurantMismatchException;
import com.fooddelivery.cartservice.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart sampleCart;

    @BeforeEach
    void setUp() {
        sampleCart = new Cart();
        sampleCart.setCartId(1L);
        sampleCart.setCustomerId("100");
        sampleCart.setRestaurantId(10L);
        sampleCart.setTotalPrice(0.0);
        sampleCart.setCartItems(new ArrayList<>());
    }

    // ─── getCartByCustomerId ───────────────────────────────────────────────────

    @Test
    @DisplayName("getCartByCustomerId: returns cart when found")
    void getCartByCustomerId_found() {
        when(cartRepository.findByCustomerId("100")).thenReturn(Optional.of(sampleCart));
        Cart result = cartService.getCartByCustomerId("100");
        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo("100");
    }

    @Test
    @DisplayName("getCartByCustomerId: throws CartNotFoundException when not found")
    void getCartByCustomerId_notFound() {
        when(cartRepository.findByCustomerId("999")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> cartService.getCartByCustomerId("999"))
                .isInstanceOf(CartNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ─── addItem ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addItem: creates new cart and adds item when no cart exists")
    void addItem_createsNewCart() {
        when(cartRepository.findByCustomerId("100")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartService.addItem("100", 10L, 50L, "Burger", 199.0, 2, "extra cheese");

        assertThat(result.getCartItems()).hasSize(1);
        assertThat(result.getCartItems().get(0).getName()).isEqualTo("Burger");
        assertThat(result.getCartItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("addItem: increments quantity when same item added again")
    void addItem_incrementsExistingItem() {
        CartItem existing = new CartItem(50L, "Burger", 199.0, 1, null);
        existing.setItemId(1L);
        sampleCart.addCartItem(existing);

        when(cartRepository.findByCustomerId("100")).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartService.addItem("100", 10L, 50L, "Burger", 199.0, 2, null);

        assertThat(result.getCartItems()).hasSize(1);
        assertThat(result.getCartItems().get(0).getQuantity()).isEqualTo(3); // 1 + 2
    }

    @Test
    @DisplayName("addItem: throws RestaurantMismatchException when item is from a different restaurant")
    void addItem_restaurantMismatch() {
        when(cartRepository.findByCustomerId("100")).thenReturn(Optional.of(sampleCart));

        assertThatThrownBy(() ->
            cartService.addItem("100", 99L, 50L, "Pizza", 299.0, 1, null))
                .isInstanceOf(RestaurantMismatchException.class);
    }

    // ─── removeItem ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("removeItem: removes item successfully")
    void removeItem_success() {
        CartItem item = new CartItem(50L, "Burger", 199.0, 1, null);
        item.setItemId(1L);
        sampleCart.addCartItem(item);

        when(cartRepository.findByCustomerId("100")).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartService.removeItem("100", 50L);
        assertThat(result.getCartItems()).isEmpty();
    }

    @Test
    @DisplayName("removeItem: throws ItemNotFoundException when item not in cart")
    void removeItem_itemNotFound() {
        when(cartRepository.findByCustomerId("100")).thenReturn(Optional.of(sampleCart));

        assertThatThrownBy(() -> cartService.removeItem("100", 999L))
                .isInstanceOf(ItemNotFoundException.class);
    }

    // ─── clearCart ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("clearCart: clears all items and resets total to zero")
    void clearCart_success() {
        CartItem item = new CartItem(50L, "Burger", 199.0, 2, null);
        sampleCart.addCartItem(item);

        when(cartRepository.findByCustomerId("100")).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        cartService.clearCart("100");

        assertThat(sampleCart.getCartItems()).isEmpty();
        assertThat(sampleCart.getTotalPrice()).isEqualTo(0.0);
    }

    // ─── cartTotal ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cartTotal: calculates total correctly")
    void cartTotal_correct() {
        CartItem item1 = new CartItem(1L, "Burger", 199.0, 2, null);
        CartItem item2 = new CartItem(2L, "Fries", 99.0, 1, null);
        sampleCart.addCartItem(item1);
        sampleCart.addCartItem(item2);

        when(cartRepository.findByCustomerId("100")).thenReturn(Optional.of(sampleCart));

        Double total = cartService.cartTotal("100");
        assertThat(total).isEqualTo(199.0 * 2 + 99.0 * 1); // 497.0
    }

    // ─── applyPromoCode ───────────────────────────────────────────────────────

    @Test
    @DisplayName("applyPromoCode: applies 10% discount for valid code")
    void applyPromoCode_valid() {
        CartItem item = new CartItem(1L, "Burger", 200.0, 1, null);
        sampleCart.addCartItem(item);

        when(cartRepository.findByCustomerId("100")).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartService.applyPromoCode("100", "SAVE10");
        assertThat(result.getTotalPrice()).isEqualTo(180.0); // 200 - 10%
    }

    @Test
    @DisplayName("applyPromoCode: throws IllegalArgumentException for invalid code")
    void applyPromoCode_invalid() {
        when(cartRepository.findByCustomerId("100")).thenReturn(Optional.of(sampleCart));

        assertThatThrownBy(() -> cartService.applyPromoCode("100", "BADCODE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BADCODE");
    }

    // ─── updateQuantity ───────────────────────────────────────────────────────

    @Test
    @DisplayName("updateQuantity: updates item quantity correctly")
    void updateQuantity_success() {
        CartItem item = new CartItem(50L, "Burger", 199.0, 1, null);
        item.setItemId(1L);
        sampleCart.addCartItem(item);

        when(cartRepository.findByCustomerId("100")).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartService.updateQuantity("100", 50L, 5);
        assertThat(result.getCartItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("updateQuantity: removes item when quantity set to 0")
    void updateQuantity_zeroRemovesItem() {
        CartItem item = new CartItem(50L, "Burger", 199.0, 2, null);
        item.setItemId(1L);
        sampleCart.addCartItem(item);

        when(cartRepository.findByCustomerId("100")).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartService.updateQuantity("100", 50L, 0);
        assertThat(result.getCartItems()).isEmpty();
    }

    // ─── changeRestaurant ─────────────────────────────────────────────────────

    @Test
    @DisplayName("changeRestaurant: clears items and sets new restaurantId")
    void changeRestaurant_clearsItemsAndSwitches() {
        CartItem item = new CartItem(50L, "Burger", 199.0, 1, null);
        sampleCart.addCartItem(item);

        when(cartRepository.findByCustomerId("100")).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = cartService.changeRestaurant("100", 99L);
        assertThat(result.getRestaurantId()).isEqualTo(99L);
        assertThat(result.getCartItems()).isEmpty();
        assertThat(result.getTotalPrice()).isEqualTo(0.0);
    }
}
