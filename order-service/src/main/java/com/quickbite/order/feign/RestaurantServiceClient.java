package com.quickbite.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.dto.RestaurantResponse;

@FeignClient(name = "restaurant-service", url = "${services.restaurant-service.url:http://localhost:8082}")
public interface RestaurantServiceClient {

    @GetMapping("/api/restaurants/{id}")
    ApiResponse<RestaurantResponse> getRestaurantById(@PathVariable("id") Long id);
}