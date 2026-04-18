package com.quickbite.restaurant.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload to update average rating (called by Review-Service).
 */
@Data 
@NoArgsConstructor 
@AllArgsConstructor
public class RatingUpdateRequest {
    @NotNull
    @DecimalMin("1.0") 
    @DecimalMax("5.0")
    private Double newRating;
}

/* ═══════════════════════════════════════════════════════════
   RESPONSE DTOs
   ═══════════════════════════════════════════════════════════ */

