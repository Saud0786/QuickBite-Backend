package com.quickbite.delivery.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// ─── Generic response wrapper ──────────────────────────────────────────────

@Data @NoArgsConstructor @AllArgsConstructor
class ApiResponseInner<T> {
    private String status;
    private String message;
    private T data;
}
