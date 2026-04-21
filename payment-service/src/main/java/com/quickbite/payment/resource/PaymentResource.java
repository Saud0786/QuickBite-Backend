package com.quickbite.payment.resource;

import com.quickbite.payment.dto.*;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.Wallet;
import com.quickbite.payment.entity.WalletStatement;
import com.quickbite.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PaymentResource — REST controller.
 *
 * ── Online Payment (CARD / UPI via Razorpay) ──
 * POST /payments/create-order          → Step 1: create Razorpay order
 * POST /payments/verify                → Step 2: verify signature + capture
 *
 * ── COD Payment ──
 * POST /payments/cod                   → record COD payment
 *
 * ── Wallet Payment ──
 * POST /payments/wallet/pay            → debit wallet for order
 *
 * ── Refund ──
 * POST /payments/refund                → refund (Razorpay / wallet / COD)
 *
 * ── Queries ──
 * GET  /payments/order/{orderId}       → payment by order
 * GET  /payments/customer/{customerId} → all payments for customer
 * PUT  /payments/status/{orderId}      → update payment status (internal)
 *
 * ── Wallet Management ──
 * GET  /wallet/{customerId}            → wallet details + balance
 * GET  /wallet/{customerId}/balance    → balance only
 * POST /wallet/topup/initiate          → Step 1: create Razorpay top-up order
 * POST /wallet/topup/confirm           → Step 2: verify + credit wallet
 * GET  /wallet/{customerId}/statements → full transaction history
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping({"", "/api"})
public class PaymentResource {

    private final PaymentService paymentService;

    // ── STEP 1: Create Razorpay Order ─────────────────────────────────────────

    @PostMapping("/payments/create-order")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("POST /payments/create-order — orderId={}, mode={}", request.getOrderId(), request.getMode());
        RazorpayOrderResponse response = paymentService.createRazorpayOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Razorpay order created. Open checkout widget.", response));
    }

    // ── STEP 2: Verify & Capture Payment ─────────────────────────────────────

    @PostMapping("/payments/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        log.info("POST /payments/verify — rzpOrderId={}", request.getRazorpayOrderId());
        Payment payment = paymentService.verifyAndCapturePayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified and captured", toResponse(payment)));
    }

    // ── COD Payment ───────────────────────────────────────────────────────────

    @PostMapping("/payments/cod")
    public ResponseEntity<ApiResponse<PaymentResponse>> processCOD(
            @Valid @RequestBody CODPaymentRequest request) {
        log.info("POST /payments/cod — orderId={}", request.getOrderId());
        Payment payment = paymentService.processCODPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("COD payment recorded", toResponse(payment)));
    }

    // ── Wallet Payment ────────────────────────────────────────────────────────

    @PostMapping("/payments/wallet/pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> payFromWallet(
            @Valid @RequestBody WalletPaymentRequest request) {
        log.info("POST /payments/wallet/pay — orderId={}, customerId={}", request.getOrderId(), request.getCustomerId());
        Payment payment = paymentService.payFromWallet(request);
        return ResponseEntity.ok(ApiResponse.success("Wallet payment successful", toResponse(payment)));
    }

    // ── Refund ────────────────────────────────────────────────────────────────

    @PostMapping("/payments/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @Valid @RequestBody RefundRequest request) {
        log.info("POST /payments/refund — orderId={}", request.getOrderId());
        Payment payment = paymentService.refundPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Refund initiated successfully", toResponse(payment)));
    }

    // ── Get Payment by Order ──────────────────────────────────────────────────

    @GetMapping("/payments/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByOrder(@PathVariable Long orderId) {
        log.info("GET /payments/order/{}", orderId);
        Payment payment = paymentService.getByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved", toResponse(payment)));
    }

    // ── Get All Payments for Customer ─────────────────────────────────────────

    @GetMapping("/payments/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getByCustomer(
            @PathVariable String customerId) {
        log.info("GET /payments/customer/{}", customerId);
        List<PaymentResponse> payments = paymentService.getByCustomerId(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved", payments));
    }

    // ── Update Payment Status (internal use) ──────────────────────────────────

    @PutMapping("/payments/status/{orderId}")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        log.info("PUT /payments/status/{} — status={}", orderId, status);
        paymentService.updatePaymentStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.success("Payment status updated", null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WALLET ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    // ── Get Wallet Details ────────────────────────────────────────────────────

    @GetMapping("/wallet/{customerId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@PathVariable String customerId) {
        log.info("GET /wallet/{}", customerId);
        Wallet wallet = paymentService.getOrCreateWallet(customerId);
        List<WalletStatement> statements = paymentService.getWalletStatements(customerId);

        WalletResponse response = new WalletResponse(
                wallet.getWalletId(),
                wallet.getCustomerId(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                statements.stream().limit(10).map(this::toStatementResponse).collect(Collectors.toList())
        );
        return ResponseEntity.ok(ApiResponse.success("Wallet retrieved", response));
    }

    // ── Get Balance Only ──────────────────────────────────────────────────────

    @GetMapping("/wallet/{customerId}/balance")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getBalance(@PathVariable String customerId) {
        log.info("GET /wallet/{}/balance", customerId);
        Double balance = paymentService.getWalletBalance(customerId);
        return ResponseEntity.ok(ApiResponse.success("Balance retrieved",
                Map.of("balance", balance)));
    }

    // ── Step 1: Initiate Wallet Top-Up via Razorpay ───────────────────────────

    @PostMapping("/wallet/topup/initiate")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> initiateTopUp(
            @Valid @RequestBody AddMoneyRequest request) {
        log.info("POST /wallet/topup/initiate — customerId={}, amount={}",
                request.getCustomerId(), request.getAmount());
        RazorpayOrderResponse response = paymentService.initiateWalletTopUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wallet top-up order created. Open checkout widget.", response));
    }

    // ── Step 2: Confirm Wallet Top-Up ────────────────────────────────────────

    @PostMapping("/wallet/topup/confirm")
    public ResponseEntity<ApiResponse<WalletResponse>> confirmTopUp(
            @Valid @RequestBody VerifyPaymentRequest request) {
        log.info("POST /wallet/topup/confirm — rzpOrderId={}", request.getRazorpayOrderId());
        Wallet wallet = paymentService.confirmWalletTopUp(request);
        WalletResponse response = new WalletResponse(
                wallet.getWalletId(),
                wallet.getCustomerId(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                null
        );
        return ResponseEntity.ok(ApiResponse.success("Wallet topped up successfully", response));
    }

    // ── Wallet Statements ─────────────────────────────────────────────────────

    @GetMapping("/wallet/{customerId}/statements")
    public ResponseEntity<ApiResponse<List<WalletStatementResponse>>> getStatements(
            @PathVariable String customerId) {
        log.info("GET /wallet/{}/statements", customerId);
        List<WalletStatementResponse> stmts = paymentService.getWalletStatements(customerId)
                .stream().map(this::toStatementResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Statements retrieved", stmts));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mappers
    // ─────────────────────────────────────────────────────────────────────────

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getPaymentId(), p.getOrderId(), p.getCustomerId(),
                p.getAmount(), p.getStatus(), p.getMode(),
                p.getRazorpayOrderId(), p.getRazorpayPaymentId(),
                p.getCurrency(), p.getPaidAt(), p.getRefundedAt(), p.getCreatedAt()
        );
    }

    private WalletStatementResponse toStatementResponse(WalletStatement s) {
        return new WalletStatementResponse(
                s.getStatementId(), s.getType(), s.getAmount(),
                s.getBalanceAfter(), s.getDescription(),
                s.getReferenceId(), s.getCreatedAt()
        );
    }
}
