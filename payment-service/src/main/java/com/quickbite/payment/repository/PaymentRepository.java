package com.quickbite.payment.repository;

import com.quickbite.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Payment> findByStatusOrderByCreatedAtDesc(String status);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    List<Payment> findByPaidAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.customerId = :customerId AND p.status = 'PAID'")
    Double sumAmountByCustomerId(@Param("customerId") String customerId);

    List<Payment> findByCustomerIdAndStatus(String customerId, String status);
}
