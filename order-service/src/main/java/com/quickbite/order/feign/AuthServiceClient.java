package com.quickbite.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.dto.UserProfileResponse;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/auth/user/{userId}")
    ApiResponse<UserProfileResponse> getUserById(
        @PathVariable("userId") String userId,
        @RequestHeader("Authorization") String authorizationHeader
    );
}
