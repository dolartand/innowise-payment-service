package com.innowise.paymentservice.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TotalAmountAggregationResult(
        BigDecimal totalAmount
) {
}
