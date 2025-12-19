package com.innowise.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreatePaymentRequest(
        @NotNull(message = "Order id is required")
        Long orderId,

        @NotNull(message = "User id is required")
        Long userId,

        @NotNull(message = "Payment amount is required")
        @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
        BigDecimal paymentAmount
) {
}
