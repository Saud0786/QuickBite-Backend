package com.quickbite.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * WalletStatement — one record per wallet transaction (CREDIT or DEBIT).
 * Forms a complete ledger for the customer's wallet history.
 */
@Entity
@Table(name = "wallet_statements", indexes = {
        @Index(name = "idx_stmt_wallet", columnList = "wallet_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "wallet")
public class WalletStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long statementId;

    /**
     * CREDIT (deposit / refund) or DEBIT (payment)
     */
    @Column(nullable = false)
    private String type;        // CREDIT | DEBIT

    @Column(nullable = false)
    private Double amount;

    /** Balance after this transaction */
    @Column(nullable = false)
    private Double balanceAfter;

    @Column(length = 500)
    private String description; // e.g. "Added via Razorpay", "Payment for Order #42"

    /** Reference — orderId or Razorpay transaction ID */
    private String referenceId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    // Convenience constructor
    public WalletStatement(String type, Double amount, Double balanceAfter,
                           String description, String referenceId, Wallet wallet) {
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.referenceId = referenceId;
        this.wallet = wallet;
        this.createdAt = LocalDateTime.now();
    }
}
