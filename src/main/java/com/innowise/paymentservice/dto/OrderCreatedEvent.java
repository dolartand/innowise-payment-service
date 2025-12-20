package com.innowise.paymentservice.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        String event
) {
    public CreatePaymentRequest toCreatePaymentRequest() {
        return CreatePaymentRequest.builder()
                .orderId(orderId)
                .userId(userId)
                .paymentAmount(this.totalAmount)
                .build();
    }
}
