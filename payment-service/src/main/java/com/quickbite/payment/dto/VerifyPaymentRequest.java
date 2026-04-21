package com.quickbite.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Step 2 — After the Razorpay checkout widget succeeds, the frontend sends
 * these three IDs back so the backend can verify the payment signature
 * and mark the payment as PAID.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPaymentRequest {

    @NotNull(message = "internalOrderId is required")
    private Long internalOrderId;       // our DB order ID

    @NotBlank(message = "razorpayOrderId is required")
    private String razorpayOrderId;     // rzp_order_xxx

    @NotBlank(message = "razorpayPaymentId is required")
    private String razorpayPaymentId;   // pay_xxx

    @NotBlank(message = "razorpaySignature is required")
    private String razorpaySignature;   // HMAC-SHA256 signature from Razorpay
}
