package com.innowise.paymentservice.dto;

import com.innowise.paymentservice.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentResponse(
        String id,
        Long orderId,
        Long userId,
        PaymentStatus status,
        LocalDateTime timestamp,
        BigDecimal totalAmount
) {
}
