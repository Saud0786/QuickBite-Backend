package com.quickbite.payment.repository;

import com.quickbite.payment.entity.WalletStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletStatementRepository extends JpaRepository<WalletStatement, Long> {

    List<WalletStatement> findByWallet_CustomerIdOrderByCreatedAtDesc(String customerId);
}
