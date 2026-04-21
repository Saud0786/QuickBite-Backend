package com.quickbite.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private Long walletId;
    private String customerId;
    private Double balance;
    private LocalDateTime createdAt;
    private List<WalletStatementResponse> recentStatements;
}
