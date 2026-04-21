package com.quickbite.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignAgentRequest {

    @NotNull(message = "deliveryAgentId is required")
    private Long deliveryAgentId;
}
