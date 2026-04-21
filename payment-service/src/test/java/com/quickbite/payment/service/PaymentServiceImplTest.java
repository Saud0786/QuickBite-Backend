package com.quickbite.payment.service;

import com.quickbite.payment.dto.*;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.Wallet;
import com.quickbite.payment.entity.WalletStatement;
import com.quickbite.payment.exception.*;
import com.quickbite.payment.repository.PaymentRepository;
import com.quickbite.payment.repository.WalletRepository;
import com.quickbite.payment.repository.WalletStatementRepository;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletStatementRepository statementRepository;
    @Mock private RazorpayClient razorpayClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment paidPayment;
    private Payment pendingPayment;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId",     "rzp_test_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_secret");
        ReflectionTestUtils.setField(paymentService, "defaultCurrency",   "INR");

        pendingPayment = new Payment();
        pendingPayment.setPaymentId(1L);
        pendingPayment.setOrderId(10L);
        pendingPayment.setCustomerId("user-100");
        pendingPayment.setAmount(500.0);
        pendingPayment.setMode("CARD");
        pendingPayment.setStatus("PENDING");
        pendingPayment.setRazorpayOrderId("rzp_order_abc");
        pendingPayment.setCurrency("INR");
        pendingPayment.setCreatedAt(LocalDateTime.now());

        paidPayment = new Payment();
        paidPayment.setPaymentId(2L);
        paidPayment.setOrderId(10L);
        paidPayment.setCustomerId("user-100");
        paidPayment.setAmount(500.0);
        paidPayment.setMode("CARD");
        paidPayment.setStatus("PAID");
        paidPayment.setRazorpayOrderId("rzp_order_abc");
        paidPayment.setRazorpayPaymentId("pay_xyz");
        paidPayment.setCurrency("INR");
        paidPayment.setPaidAt(LocalDateTime.now());
        paidPayment.setCreatedAt(LocalDateTime.now());

        wallet = new Wallet();
        wallet.setWalletId(1L);
        wallet.setCustomerId("user-100");
        wallet.setBalance(1000.0);
        wallet.setStatements(new ArrayList<>());
        wallet.setCreatedAt(LocalDateTime.now());
    }

    // ─── processCODPayment ────────────────────────────────────────────────────

    @Test
    @DisplayName("processCODPayment: creates PENDING payment record")
    void processCOD_success() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment result = paymentService.processCODPayment(
            new CODPaymentRequest(10L, "user-100", 500.0));

        assertThat(result.getMode()).isEqualTo("COD");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getAmount()).isEqualTo(500.0);
    }

    @Test
    @DisplayName("processCODPayment: throws when order already PAID")
    void processCOD_alreadyPaid() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(paidPayment));
        assertThatThrownBy(() ->
            paymentService.processCODPayment(new CODPaymentRequest(10L, "user-100", 500.0)))
                .isInstanceOf(PaymentAlreadyProcessedException.class);
    }

    // ─── payFromWallet ────────────────────────────────────────────────────────

    @Test
    @DisplayName("payFromWallet: debits wallet and creates PAID payment")
    void payFromWallet_success() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(walletRepository.findByCustomerId("user-100")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(statementRepository.save(any(WalletStatement.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment result = paymentService.payFromWallet(
            new WalletPaymentRequest(10L, "user-100", 400.0));

        assertThat(result.getMode()).isEqualTo("WALLET");
        assertThat(result.getStatus()).isEqualTo("PAID");
        assertThat(wallet.getBalance()).isEqualTo(600.0); // 1000 - 400
    }

    @Test
    @DisplayName("payFromWallet: throws InsufficientBalanceException when balance too low")
    void payFromWallet_insufficient() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(walletRepository.findByCustomerId("user-100")).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() ->
            paymentService.payFromWallet(new WalletPaymentRequest(10L, "user-100", 9999.0)))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    @DisplayName("payFromWallet: throws when order already PAID")
    void payFromWallet_alreadyPaid() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(paidPayment));
        assertThatThrownBy(() ->
            paymentService.payFromWallet(new WalletPaymentRequest(10L, "user-100", 400.0)))
                .isInstanceOf(PaymentAlreadyProcessedException.class);
    }

    // ─── refundPayment ────────────────────────────────────────────────────────

    @Test
    @DisplayName("refundPayment: WALLET mode credits money back to wallet")
    void refundWallet_success() {
        paidPayment.setMode("WALLET");
        paidPayment.setCustomerId("user-100");
        paidPayment.setAmount(400.0);
        wallet.setBalance(600.0);

        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(paidPayment));
        when(walletRepository.findByCustomerId("user-100")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(statementRepository.save(any(WalletStatement.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment result = paymentService.refundPayment(new RefundRequest(10L, "Test refund"));

        assertThat(result.getStatus()).isEqualTo("REFUNDED");
        assertThat(wallet.getBalance()).isEqualTo(1000.0); // 600 + 400 refunded
    }

    @Test
    @DisplayName("refundPayment: throws when payment already REFUNDED")
    void refund_alreadyRefunded() {
        paidPayment.setStatus("REFUNDED");
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(paidPayment));

        assertThatThrownBy(() ->
                paymentService.refundPayment(new RefundRequest(10L, "reason")))
                .isInstanceOf(PaymentAlreadyProcessedException.class);
    }

    @Test
    @DisplayName("refundPayment: throws when payment is PENDING (not PAID)")
    void refund_notPaid() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() ->
                paymentService.refundPayment(new RefundRequest(10L, "reason")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PAID");
    }

    @Test
    @DisplayName("refundPayment: COD mode marks REFUNDED without Razorpay call")
    void refundCOD_success() {
        paidPayment.setMode("COD");
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(paidPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment result = paymentService.refundPayment(new RefundRequest(10L, "COD refund"));

        assertThat(result.getStatus()).isEqualTo("REFUNDED");
        verifyNoInteractions(razorpayClient);
    }

    // ─── getByOrderId ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByOrderId: returns payment when found")
    void getByOrderId_found() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(paidPayment));
        Payment result = paymentService.getByOrderId(10L);
        assertThat(result.getOrderId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getByOrderId: throws PaymentNotFoundException when not found")
    void getByOrderId_notFound() {
        when(paymentRepository.findByOrderId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.getByOrderId(99L))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    // ─── getOrCreateWallet ────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrCreateWallet: returns existing wallet")
    void getWallet_existing() {
        when(walletRepository.findByCustomerId("user-100")).thenReturn(Optional.of(wallet));
        Wallet result = paymentService.getOrCreateWallet("user-100");
        assertThat(result.getBalance()).isEqualTo(1000.0);
        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateWallet: creates new wallet when none exists")
    void getWallet_create() {
        when(walletRepository.findByCustomerId("user-200")).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> {
            Wallet w = inv.getArgument(0);
            w.setWalletId(99L);
            return w;
        });
        Wallet result = paymentService.getOrCreateWallet("user-200");
        assertThat(result.getCustomerId()).isEqualTo("user-200");
        assertThat(result.getBalance()).isEqualTo(0.0);
    }

    // ─── getWalletBalance ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getWalletBalance: returns correct balance")
    void getWalletBalance_correct() {
        when(walletRepository.findByCustomerId("user-100")).thenReturn(Optional.of(wallet));
        assertThat(paymentService.getWalletBalance("user-100")).isEqualTo(1000.0);
    }

    // ─── updatePaymentStatus ──────────────────────────────────────────────────

    @Test
    @DisplayName("updatePaymentStatus: sets paidAt when status becomes PAID")
    void updatePaymentStatus_toPaid() {
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.updatePaymentStatus(10L, "PAID");

        assertThat(pendingPayment.getStatus()).isEqualTo("PAID");
        assertThat(pendingPayment.getPaidAt()).isNotNull();
    }

    // ─── getWalletStatements ──────────────────────────────────────────────────

    @Test
    @DisplayName("getWalletStatements: returns list of statements for customer")
    void getWalletStatements() {
        WalletStatement stmt = new WalletStatement(
                "CREDIT", 500.0, 500.0, "Top-up", "pay_abc", wallet);
        stmt.setStatementId(1L);
        stmt.setCreatedAt(LocalDateTime.now());

        when(statementRepository.findByWallet_CustomerIdOrderByCreatedAtDesc("user-100"))
                .thenReturn(List.of(stmt));

        List<WalletStatement> result = paymentService.getWalletStatements("user-100");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("CREDIT");
    }

    // ─── Wallet debit/credit helpers ──────────────────────────────────────────

    @Test
    @DisplayName("Wallet.hasSufficientBalance: returns false when balance too low")
    void wallet_insufficient() {
        assertThat(wallet.hasSufficientBalance(1001.0)).isFalse();
    }

    @Test
    @DisplayName("Wallet.hasSufficientBalance: returns true for exact balance")
    void wallet_exactBalance() {
        assertThat(wallet.hasSufficientBalance(1000.0)).isTrue();
    }

    @Test
    @DisplayName("Wallet.debit: throws IllegalStateException on overdraft")
    void wallet_debit_overdraft() {
        assertThatThrownBy(() -> wallet.debit(2000.0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient");
    }
}
