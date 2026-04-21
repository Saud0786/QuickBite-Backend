package com.quickbite.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletStatementResponse {
    private Long statementId;
    private String type;           // CREDIT | DEBIT
    private Double amount;
    private Double balanceAfter;
    private String description;
    private String referenceId;
    private LocalDateTime createdAt;
}
