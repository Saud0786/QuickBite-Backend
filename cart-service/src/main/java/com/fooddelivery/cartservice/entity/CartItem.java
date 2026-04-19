package com.fooddelivery.cartservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "cart")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @Column(nullable = false)
    private Long menuItemId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double price;  // price snapshot at the time of add

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 500)
    private String customization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // Constructor without cart (cart set separately)
    public CartItem(Long menuItemId, String name, Double price, Integer quantity, String customization) {
        this.menuItemId = menuItemId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.customization = customization;
    }

    // Getters for computed values
    public String getName() {
        return name;
    }

    public Double getPrice() {
        return price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getCustomization() {
        return customization;
    }

    // Setter for quantity (used in update)
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    // Setter for customization
    public void setCustomization(String customization) {
        this.customization = customization;
    }

    @Override
    public String toString() {
        return "CartItem{" +
                "itemId=" + itemId +
                ", menuItemId=" + menuItemId +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", customization='" + customization + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem cartItem)) return false;
        return Objects.equals(itemId, cartItem.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }
}
