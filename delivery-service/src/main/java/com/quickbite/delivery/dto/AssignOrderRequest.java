package com.quickbite.delivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignOrderRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;
}
