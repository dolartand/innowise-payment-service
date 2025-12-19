package com.innowise.paymentservice.dto;

import com.innowise.paymentservice.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentEvent(
        String paymentId,
        Long orderId,
        Long userId,
        PaymentStatus status,
        BigDecimal paymentAmount,
        LocalDateTime timestamp,
        String eventType
) {
    public static PaymentEvent fromPaymentResponse(PaymentResponse response) {
        return PaymentEvent.builder()
                .paymentId(response.id())
                .orderId(response.orderId())
                .userId(response.userId())
                .status(response.status())
                .paymentAmount(response.totalAmount())
                .timestamp(response.timestamp())
                .eventType("CREATE_PAYMENT")
                .build();
    }
}
