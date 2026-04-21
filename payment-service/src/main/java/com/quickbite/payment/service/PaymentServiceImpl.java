package com.quickbite.payment.service;

import com.quickbite.payment.dto.*;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.Wallet;
import com.quickbite.payment.entity.WalletStatement;
import com.quickbite.payment.exception.*;
import com.quickbite.payment.repository.PaymentRepository;
import com.quickbite.payment.repository.WalletRepository;
import com.quickbite.payment.repository.WalletStatementRepository;
import com.razorpay.Order;
import com.razorpay.Refund;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final WalletStatementRepository statementRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency:INR}")
    private String defaultCurrency;

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1: Create Razorpay Order (CARD / UPI)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(CreateOrderRequest request) {
        log.debug("createRazorpayOrder: orderId={}, amount={}, mode={}",
                request.getOrderId(), request.getAmount(), request.getMode());

        validateOnlineMode(request.getMode());

        Payment existingPayment = paymentRepository.findByOrderId(request.getOrderId()).orElse(null);

        // Prevent duplicate payment for an already-paid order.
        if (existingPayment != null && "PAID".equalsIgnoreCase(existingPayment.getStatus())) {
            throw new PaymentAlreadyProcessedException(
                "Order " + request.getOrderId() + " has already been paid.");
        }

        // If a pending online payment already exists for this order, reuse it for retry flow.
        if (existingPayment != null
            && "PENDING".equalsIgnoreCase(existingPayment.getStatus())
            && existingPayment.getRazorpayOrderId() != null
            && !existingPayment.getRazorpayOrderId().isBlank()) {
            log.info("Reusing pending Razorpay order: rzpOrderId={} for internalOrderId={}",
                existingPayment.getRazorpayOrderId(), request.getOrderId());

            return new RazorpayOrderResponse(
                request.getOrderId(),
                request.getCustomerId(),
                existingPayment.getRazorpayOrderId(),
                existingPayment.getAmount(),
                existingPayment.getCurrency(),
                existingPayment.getMode(),
                "created",
                razorpayKeyId
            );
        }

        try {
            // Razorpay expects amount in paise (1 INR = 100 paise)
            int amountInPaise = (int) Math.round(request.getAmount() * 100);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", request.getCurrency() != null
                    ? request.getCurrency() : defaultCurrency);
            // Keep receipt unique to avoid duplicate conflicts on retries.
            orderRequest.put("receipt", "receipt_order_" + request.getOrderId() + "_" + System.currentTimeMillis());
            orderRequest.put("payment_capture", 1); // auto-capture

            // Add notes for traceability in Razorpay dashboard
            JSONObject notes = new JSONObject();
            notes.put("internalOrderId", request.getOrderId().toString());
            notes.put("customerId", request.getCustomerId().toString());
            notes.put("mode", request.getMode());
            orderRequest.put("notes", notes);

            Order rzpOrder = razorpayClient.orders.create(orderRequest);
            String rzpOrderId = rzpOrder.get("id");

            log.info("Razorpay order created: rzpOrderId={} for internalOrderId={}",
                    rzpOrderId, request.getOrderId());

            // Persist a PENDING payment record
            Payment payment = existingPayment != null ? existingPayment : new Payment();
            payment.setOrderId(request.getOrderId());
            payment.setCustomerId(request.getCustomerId());
            payment.setAmount(request.getAmount());
            payment.setMode(request.getMode().toUpperCase());
            payment.setStatus("PENDING");
            payment.setRazorpayOrderId(rzpOrderId);
            payment.setCurrency(request.getCurrency() != null
                    ? request.getCurrency() : defaultCurrency);
            paymentRepository.save(payment);

            return new RazorpayOrderResponse(
                    request.getOrderId(),
                    request.getCustomerId(),
                    rzpOrderId,
                    request.getAmount(),
                    payment.getCurrency(),
                    request.getMode().toUpperCase(),
                    "created",
                    razorpayKeyId   // sent to frontend for the checkout widget
            );

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Razorpay create-order failed: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2: Verify Razorpay Signature + Capture Payment
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Payment verifyAndCapturePayment(VerifyPaymentRequest request) {
        log.debug("verifyAndCapturePayment: rzpOrderId={}, rzpPaymentId={}",
                request.getRazorpayOrderId(), request.getRazorpayPaymentId());

        // 1. Verify HMAC-SHA256 signature
        verifyRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        // 2. Fetch pending payment record
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No pending payment found for Razorpay order: "
                                + request.getRazorpayOrderId()));

        if ("PAID".equals(payment.getStatus())) {
            throw new PaymentAlreadyProcessedException(
                    "Payment already captured for order: " + payment.getOrderId());
        }

        // 3. Mark as PAID
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        log.info("Payment PAID: paymentId={}, orderId={}, amount={}",
                saved.getPaymentId(), saved.getOrderId(), saved.getAmount());
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COD Payment
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Payment processCODPayment(CODPaymentRequest request) {
        log.debug("processCODPayment: orderId={}, customerId={}",
                request.getOrderId(), request.getCustomerId());

        paymentRepository.findByOrderId(request.getOrderId()).ifPresent(p -> {
            if ("PAID".equals(p.getStatus())) {
                throw new PaymentAlreadyProcessedException(
                        "Order " + request.getOrderId() + " already has a PAID record.");
            }
        });

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setCustomerId(request.getCustomerId());
        payment.setAmount(request.getAmount());
        payment.setMode("COD");
        // COD is PENDING until delivery agent marks it collected
        payment.setStatus("PENDING");
        payment.setCurrency(defaultCurrency);

        Payment saved = paymentRepository.save(payment);
        log.info("COD payment recorded: paymentId={}, orderId={}", saved.getPaymentId(), saved.getOrderId());
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wallet Payment
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Payment payFromWallet(WalletPaymentRequest request) {
        log.debug("payFromWallet: orderId={}, customerId={}, amount={}",
                request.getOrderId(), request.getCustomerId(), request.getAmount());

        paymentRepository.findByOrderId(request.getOrderId()).ifPresent(p -> {
            if ("PAID".equals(p.getStatus())) {
                throw new PaymentAlreadyProcessedException(
                        "Order " + request.getOrderId() + " already paid.");
            }
        });

        Wallet wallet = getOrCreateWallet(request.getCustomerId());

        if (!wallet.hasSufficientBalance(request.getAmount())) {
            throw new InsufficientBalanceException(
                    "Insufficient wallet balance. Available: ₹" + wallet.getBalance()
                            + ", Required: ₹" + request.getAmount());
        }

        // Debit wallet
        wallet.debit(request.getAmount());
        walletRepository.save(wallet);

        // Record statement
        WalletStatement stmt = new WalletStatement(
                "DEBIT",
                request.getAmount(),
                wallet.getBalance(),
                "Payment for Order #" + request.getOrderId(),
                request.getOrderId().toString(),
                wallet
        );
        statementRepository.save(stmt);

        // Create PAID payment record
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setCustomerId(request.getCustomerId());
        payment.setAmount(request.getAmount());
        payment.setMode("WALLET");
        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        payment.setCurrency(defaultCurrency);

        Payment saved = paymentRepository.save(payment);
        log.info("Wallet payment PAID: paymentId={}, orderId={}, walletBalance={}",
                saved.getPaymentId(), saved.getOrderId(), wallet.getBalance());
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Refund
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Payment refundPayment(RefundRequest request) {
        log.debug("refundPayment: orderId={}", request.getOrderId());

        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No payment found for order: " + request.getOrderId()));

        if ("REFUNDED".equals(payment.getStatus())) {
            throw new PaymentAlreadyProcessedException(
                    "Payment for order " + request.getOrderId() + " already refunded.");
        }
        if (!"PAID".equals(payment.getStatus())) {
            throw new IllegalArgumentException(
                    "Only PAID payments can be refunded. Current status: " + payment.getStatus());
        }

        String mode = payment.getMode();

        switch (mode) {
            case "CARD", "UPI" -> {
                // Razorpay refund
                try {
                    int amountInPaise = (int) Math.round(payment.getAmount() * 100);
                    JSONObject refundRequest = new JSONObject();
                    refundRequest.put("amount", amountInPaise);
                    refundRequest.put("speed", "normal");
                    refundRequest.put("notes", new JSONObject()
                            .put("reason", request.getReason())
                            .put("internalOrderId", payment.getOrderId()));

                    Refund refund = razorpayClient.payments.refund(
                            payment.getRazorpayPaymentId(), refundRequest);

                    payment.setRazorpayRefundId(refund.get("id"));
                    log.info("Razorpay refund initiated: refundId={}, paymentId={}",
                            refund.get("id"), payment.getRazorpayPaymentId());
                } catch (RazorpayException e) {
                    log.error("Razorpay refund failed: {}", e.getMessage(), e);
                    throw new RuntimeException("Razorpay refund failed: " + e.getMessage(), e);
                }
            }
            case "WALLET" -> {
                // Credit back to wallet
                Wallet wallet = getOrCreateWallet(payment.getCustomerId());
                wallet.credit(payment.getAmount());
                walletRepository.save(wallet);

                WalletStatement stmt = new WalletStatement(
                        "CREDIT",
                        payment.getAmount(),
                        wallet.getBalance(),
                        "Refund for Order #" + payment.getOrderId(),
                        payment.getOrderId().toString(),
                        wallet
                );
                statementRepository.save(stmt);
                log.info("Wallet refund credited: customerId={}, amount={}",
                        payment.getCustomerId(), payment.getAmount());
            }
            case "COD" -> {
                // COD refunds are physical — just mark REFUNDED in the system
                log.info("COD refund marked for orderId={} — physical cash refund required",
                        payment.getOrderId());
            }
            default -> throw new IllegalArgumentException("Unknown payment mode: " + mode);
        }

        payment.setStatus("REFUNDED");
        payment.setRefundedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        log.info("Payment REFUNDED: paymentId={}, orderId={}", saved.getPaymentId(), saved.getOrderId());
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Payment getByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No payment found for order: " + orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getByCustomerId(String customerId) {
        return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    @Transactional
    public void updatePaymentStatus(Long orderId, String status) {
        Payment payment = getByOrderId(orderId);
        payment.setStatus(status.toUpperCase());
        if ("PAID".equals(status.toUpperCase())) {
            payment.setPaidAt(LocalDateTime.now());
        }
        paymentRepository.save(payment);
        log.info("Payment status updated: orderId={}, status={}", orderId, status);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wallet Operations
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Wallet getOrCreateWallet(String customerId) {
        return walletRepository.findByCustomerId(customerId).orElseGet(() -> {
            log.info("Creating new wallet for customerId={}", customerId);
            Wallet w = new Wallet();
            w.setCustomerId(customerId);
            w.setBalance(0.0);
            return walletRepository.save(w);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Double getWalletBalance(String customerId) {
        return getOrCreateWallet(customerId).getBalance();
    }

    @Override
    @Transactional
    public RazorpayOrderResponse initiateWalletTopUp(AddMoneyRequest request) {
        log.debug("initiateWalletTopUp: customerId={}, amount={}",
                request.getCustomerId(), request.getAmount());
        try {
            int amountInPaise = (int) Math.round(request.getAmount() * 100);

            JSONObject orderReq = new JSONObject();
            orderReq.put("amount", amountInPaise);
            orderReq.put("currency", request.getCurrency() != null
                    ? request.getCurrency() : defaultCurrency);
            orderReq.put("receipt", "wallet_topup_" + request.getCustomerId()
                    + "_" + System.currentTimeMillis());

            JSONObject notes = new JSONObject();
            notes.put("type", "WALLET_TOPUP");
            notes.put("customerId", request.getCustomerId().toString());
            orderReq.put("notes", notes);

            Order rzpOrder = razorpayClient.orders.create(orderReq);
            String rzpOrderId = rzpOrder.get("id");

            log.info("Razorpay wallet top-up order created: rzpOrderId={}", rzpOrderId);

            return new RazorpayOrderResponse(
                    null,                   // no internal orderId for top-up
                    request.getCustomerId(),
                    rzpOrderId,
                    request.getAmount(),
                    request.getCurrency() != null ? request.getCurrency() : defaultCurrency,
                    "WALLET_TOPUP",
                    "created",
                    razorpayKeyId
            );

        } catch (RazorpayException e) {
            log.error("Razorpay wallet top-up order creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initiate wallet top-up: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Wallet confirmWalletTopUp(VerifyPaymentRequest request) {
        log.debug("confirmWalletTopUp: rzpOrderId={}", request.getRazorpayOrderId());

        // Verify Razorpay signature
        verifyRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        // Fetch the Razorpay payment to get amount
        double amount;
        try {
            com.razorpay.Payment rzpPayment =
                    razorpayClient.payments.fetch(request.getRazorpayPaymentId());
            // amount from Razorpay is in paise
            amount = ((Number) rzpPayment.get("amount")).doubleValue() / 100.0;
        } catch (RazorpayException e) {
            log.error("Could not fetch Razorpay payment details: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch payment amount from Razorpay: " + e.getMessage(), e);
        }

        Wallet wallet = getOrCreateWallet(extractCustomerIdFromRzpOrder(request.getRazorpayOrderId()));

        wallet.credit(amount);
        walletRepository.save(wallet);

        WalletStatement stmt = new WalletStatement(
                "CREDIT",
                amount,
                wallet.getBalance(),
                "Wallet top-up via Razorpay",
                request.getRazorpayPaymentId(),
                wallet
        );
        statementRepository.save(stmt);

        log.info("Wallet top-up confirmed: customerId={}, amount=₹{}, newBalance=₹{}",
                wallet.getCustomerId(), amount, wallet.getBalance());
        return wallet;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletStatement> getWalletStatements(String customerId) {
        return statementRepository.findByWallet_CustomerIdOrderByCreatedAtDesc(customerId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verify Razorpay HMAC-SHA256 signature.
     * Formula: HMAC_SHA256(razorpayOrderId + "|" + razorpayPaymentId, keySecret)
     */
    private void verifyRazorpaySignature(String razorpayOrderId,
                                          String razorpayPaymentId,
                                          String signature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", signature);

            boolean valid = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
            if (!valid) {
                throw new InvalidSignatureException(
                        "Razorpay payment signature verification failed. " +
                        "Possible tampering detected for paymentId: " + razorpayPaymentId);
            }
            log.debug("Razorpay signature verified successfully for paymentId={}", razorpayPaymentId);
        } catch (RazorpayException e) {
            throw new InvalidSignatureException(
                    "Signature verification error: " + e.getMessage());
        }
    }

    private void validateOnlineMode(String mode) {
        if (!"CARD".equalsIgnoreCase(mode) && !"UPI".equalsIgnoreCase(mode)) {
            throw new IllegalArgumentException(
                "Razorpay order can only be created for CARD or UPI mode. Got: " + mode);
        }
    }

    /**
     * Fallback: extract customerId from Razorpay order notes when
     * internalOrderId is null (wallet top-up scenario).
     */
    private String extractCustomerIdFromRzpOrder(String rzpOrderId) {
        try {
            Order order = razorpayClient.orders.fetch(rzpOrderId);
            JSONObject notes = order.get("notes");
            return notes.getString("customerId");
        } catch (Exception e) {
            throw new RuntimeException("Could not extract customerId from Razorpay order: " + rzpOrderId, e);
        }
    }
}
