package com.quickbite.payment.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RazorpayConfig {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    /**
     * Creates a singleton RazorpayClient using the configured API credentials.
     * The client is thread-safe and can be injected wherever Razorpay calls are needed.
     */
    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        log.info("Initialising Razorpay client with keyId={}", keyId);
        return new RazorpayClient(keyId, keySecret);
    }
}
