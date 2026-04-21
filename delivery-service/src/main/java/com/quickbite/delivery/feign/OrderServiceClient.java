package com.quickbite.delivery.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "order-service", url = "${services.order-service.url:http://localhost:8085}")
public interface OrderServiceClient {

    @PutMapping("/api/orders/{orderId}/delivery-complete")
    void markDelivered(
            @PathVariable("orderId") Long orderId,
            @RequestBody Map<String, Long> payload
    );
}