package com.quickbite.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Wallet — one wallet per customer.
 * Tracks running balance and a full history of WalletStatements (deposit / debit).
 */
@Entity
@Table(name = "wallets", indexes = {
        @Index(name = "idx_wallet_customer", columnList = "customerId", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "statements")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletId;

    @Column(nullable = false, unique = true)
    private String customerId;

    @Column(nullable = false)
    private Double balance = 0.0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WalletStatement> statements = new ArrayList<>();

    // ── Business helpers ──────────────────────────────────────────────────────

    public boolean hasSufficientBalance(Double amount) {
        return this.balance >= amount;
    }

    public void credit(Double amount) {
        this.balance += amount;
    }

    public void debit(Double amount) {
        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient wallet balance. " +
                    "Available: ₹" + this.balance + ", Required: ₹" + amount);
        }
        this.balance -= amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wallet w)) return false;
        return Objects.equals(walletId, w.walletId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(walletId);
    }
}
