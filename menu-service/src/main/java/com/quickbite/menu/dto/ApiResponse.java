package com.quickbite.menu.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response wrapper — mirrors your auth-service pattern.
 */
@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
public class ApiResponse<T> {
	
    private boolean success;
    private String message;
    private T data;
    private int statusCode;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true).message(message).data(data)
                .statusCode(200).timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true).message(message).data(data)
                .statusCode(201).timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return ApiResponse.<T>builder()
                .success(false).message(message)
                .statusCode(statusCode).timestamp(LocalDateTime.now())
                .build();
    }
}
