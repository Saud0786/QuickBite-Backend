package com.quickbite.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned to the frontend after creating a Razorpay order.
 * The frontend uses these fields to initialise the Razorpay checkout widget.
 *
 * Frontend usage (JS):
 *   var options = {
 *     key:        response.keyId,
 *     amount:     response.amount,
 *     currency:   response.currency,
 *     order_id:   response.razorpayOrderId,
 *     ...
 *   };
 *   var rzp = new Razorpay(options);
 *   rzp.open();
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponse {

    private Long internalOrderId;       // our DB order ID
    private String customerId;
    private String razorpayOrderId;     // rzp_order_xxx  — pass to Razorpay widget
    private Double amount;              // in INR (NOT paise — frontend converts)
    private String currency;
    private String mode;
    private String status;             // "created"
    private String keyId;              // Razorpay public key for the frontend
}
