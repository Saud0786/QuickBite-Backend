package com.quickbite.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

/**
 * OrderItem — immutable snapshot of a CartItem saved at the time of order placement.
 * Price and name are captured so historical orders remain accurate even if the menu changes.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "order")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @Column(nullable = false)
    private Long menuItemId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double price;       // price snapshot at time of order

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 500)
    private String customization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Constructor for building from cart items
    public OrderItem(Long menuItemId, String name, Double price,
                     Integer quantity, String customization) {
        this.menuItemId = menuItemId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.customization = customization;
    }

    public Double getSubtotal() {
        return this.price * this.quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem item)) return false;
        return Objects.equals(orderItemId, item.orderItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderItemId);
    }
}
