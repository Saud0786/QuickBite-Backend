package com.quickbite.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "orderItems")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private Long restaurantId;

    // Assigned after order is confirmed
    private Long deliveryAgentId;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private Double discount = 0.0;

    @Column(nullable = false)
    private Double finalAmount;

    @Column(nullable = false)
    private String modeOfPayment;  // COD / CARD / UPI / WALLET

    /**
     * Order status lifecycle:
        * PLACED → CONFIRMED → PREPARING → PACKING → PICKED_UP → DELIVERED
     *                          ↓
     *                      CANCELLED
     */
    @Column(nullable = false)
    private String orderStatus = "PLACED";

    @Column(nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    private LocalDateTime estimatedDelivery;

    @Column(nullable = false)
    private String deliveryAddress;

    @Column(length = 500)
    private String specialInstructions;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> orderItems = new ArrayList<>();

    // ── Business helpers ──────────────────────────────────────────────────────

    public void addOrderItem(OrderItem item) {
        item.setOrder(this);
        this.orderItems.add(item);
    }

    public boolean isCancellable() {
        return "PLACED".equals(this.orderStatus)
                || "CONFIRMED".equals(this.orderStatus)
                || "PREPARING".equals(this.orderStatus)
                || "PACKING".equals(this.orderStatus);
    }

    public boolean isDelivered() {
        return "DELIVERED".equals(this.orderStatus);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
}
