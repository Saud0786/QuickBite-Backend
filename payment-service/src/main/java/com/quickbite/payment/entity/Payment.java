package com.quickbite.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Payment — one record per order payment attempt.
 *
 * Status lifecycle:
 *   PENDING → PAID       (success)
 *   PENDING → FAILED     (gateway failure)
 *   PAID    → REFUNDED   (on order cancellation)
 *
 * Mode: CARD | UPI | WALLET | COD
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order",       columnList = "orderId"),
        @Index(name = "idx_payment_customer",    columnList = "customerId"),
        @Index(name = "idx_payment_transaction", columnList = "razorpayOrderId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private Double amount;

    /**
     * PENDING / PAID / REFUNDED / FAILED
     */
    @Column(nullable = false)
    private String status = "PENDING";

    /**
     * CARD / UPI / WALLET / COD
     */
    @Column(nullable = false)
    private String mode;

    /** Razorpay Order ID (rzp_order_xxx) — created before checkout */
    private String razorpayOrderId;

    /** Razorpay Payment ID (pay_xxx) — returned after successful payment */
    private String razorpayPaymentId;

    /** Razorpay Refund ID (rfnd_xxx) — set when refund is initiated */
    private String razorpayRefundId;

    @Column(nullable = false)
    private String currency = "INR";

    private LocalDateTime paidAt;

    private LocalDateTime refundedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment p)) return false;
        return Objects.equals(paymentId, p.paymentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId);
    }
}
