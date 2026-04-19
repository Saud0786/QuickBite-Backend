package com.fooddelivery.cartservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "cartItems")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @Column(nullable = false, unique = true)
    private String customerId;

    @Column(nullable = false)
    private Long restaurantId;

    @Column(nullable = false)
    private Double totalPrice = 0.0;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CartItem> cartItems = new ArrayList<>();

    // Constructor for creating a new cart
    public Cart(String customerId, List<CartItem> cartItems) {
        this.customerId = customerId;
        this.cartItems = cartItems;
    }

    // Business method: add a CartItem to this cart
    public void addCartItem(CartItem item) {
        item.setCart(this);
        this.cartItems.add(item);
        recalculateTotal();
    }

    // Business method: remove a CartItem from this cart
    public void removeCartItem(CartItem item) {
        this.cartItems.remove(item);
        item.setCart(null);
        recalculateTotal();
    }

    // Business method: get cart item by menuItemId
    public CartItem getCartItemByMenuItemId(Long menuItemId) {
        return this.cartItems.stream()
                .filter(item -> item.getMenuItemId().equals(menuItemId))
                .findFirst()
                .orElse(null);
    }

    // Recalculate total price from all items
    public void recalculateTotal() {
        this.totalPrice = this.cartItems.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }

    // Clear all items in the cart
    public void clearCart() {
        this.cartItems.clear();
        this.totalPrice = 0.0;
    }

    // Get total price
    public Double getTotalPrice() {
        recalculateTotal();
        return this.totalPrice;
    }

    // Set total price manually (for promo code, etc.)
    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cart cart)) return false;
        return Objects.equals(cartId, cart.cartId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartId);
    }
}
