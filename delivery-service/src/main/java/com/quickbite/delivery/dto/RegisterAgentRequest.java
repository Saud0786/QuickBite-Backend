package com.quickbite.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterAgentRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "restaurantId is required")
    private Long restaurantId;

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String phone;

    @NotBlank(message = "vehicleType is required")
    private String vehicleType;    // BIKE / BICYCLE / SCOOTER / CAR

    @NotBlank(message = "vehicleNumber is required")
    private String vehicleNumber;
}
