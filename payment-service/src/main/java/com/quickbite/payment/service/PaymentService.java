package com.quickbite.payment.service;

import com.quickbite.payment.dto.*;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.Wallet;
import com.quickbite.payment.entity.WalletStatement;

import java.util.List;

/**
 * PaymentService — declares all payment processing, refund,
 * wallet top-up, wallet payment, and statement retrieval operations.
 */
public interface PaymentService {

    // ── Razorpay Online Payment (CARD / UPI) ──────────────────────────────────

    /**
     * Step 1: Create a Razorpay order and return the order ID + key to the frontend
     * so it can open the Razorpay checkout widget.
     */
    RazorpayOrderResponse createRazorpayOrder(CreateOrderRequest request);

    /**
     * Step 2: Verify the Razorpay signature after the frontend completes checkout,
     * then mark the payment as PAID in our DB.
     */
    Payment verifyAndCapturePayment(VerifyPaymentRequest request);

    // ── COD Payment ───────────────────────────────────────────────────────────

    /**
     * Record a Cash on Delivery payment.
     * No Razorpay involved — status goes directly to PAID on delivery confirmation.
     */
    Payment processCODPayment(CODPaymentRequest request);

    // ── Wallet Payment ────────────────────────────────────────────────────────

    /**
     * Debit the customer's wallet to pay for an order.
     * Validates sufficient balance before debiting.
     */
    Payment payFromWallet(WalletPaymentRequest request);

    // ── Refund ────────────────────────────────────────────────────────────────

    /**
     * Refund a payment.
     * - CARD/UPI: triggers Razorpay refund API
     * - WALLET: credits the amount back to the wallet
     * - COD: marks as REFUNDED (physical cash refund coordinated separately)
     */
    Payment refundPayment(RefundRequest request);

    // ── Queries ───────────────────────────────────────────────────────────────

    Payment getByOrderId(Long orderId);

    List<Payment> getByCustomerId(String customerId);

    void updatePaymentStatus(Long orderId, String status);

    // ── Wallet CRUD ───────────────────────────────────────────────────────────

    /**
     * Get or auto-create a wallet for a customer.
     */
    Wallet getOrCreateWallet(String customerId);

    Double getWalletBalance(String customerId);

    /**
     * Add money to wallet via Razorpay (CARD / UPI).
     * Creates a Razorpay order — frontend completes payment, then calls
     * confirmWalletTopUp to credit the wallet.
     */
    RazorpayOrderResponse initiateWalletTopUp(AddMoneyRequest request);

    /**
     * Called after the frontend completes the Razorpay top-up payment.
     * Verifies signature and credits the wallet.
     */
    Wallet confirmWalletTopUp(VerifyPaymentRequest request);

    List<WalletStatement> getWalletStatements(String customerId);
}
