package com.innowise.paymentservice.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentSummaryResponse(
        BigDecimal totalAmount,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        Long userId,
        Long paymentsCount
) {
}
