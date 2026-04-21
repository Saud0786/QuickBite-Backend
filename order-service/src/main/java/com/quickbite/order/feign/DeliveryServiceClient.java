package com.quickbite.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "delivery-service", url = "${services.delivery-service.url:http://localhost:8087}")
public interface DeliveryServiceClient {

    @PostMapping("/api/agents/{agentId}/assign-order")
    void assignOrder(
            @PathVariable("agentId") Long agentId,
            @RequestBody Map<String, Long> payload
    );
}
