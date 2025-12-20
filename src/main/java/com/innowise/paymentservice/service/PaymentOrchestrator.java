package com.innowise.paymentservice.service;

import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentEvent;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.kafka.producer.PaymentEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Coordinates payment creation and event sending
 * Used for testing and direct calls, not via Kafka
 * Optional, can be used PaymentService + PaymentEventProducer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOrchestrator {

    private final PaymentService paymentService;
    private final PaymentEventProducer producer;

    public PaymentResponse createPaymentAndSendEvent(CreatePaymentRequest request) {
        log.info("Orchestrating payment creation for orderId={}", request.orderId());

        PaymentResponse response = paymentService.createPayment(request);

        PaymentEvent event = PaymentEvent.fromPaymentResponse(response);
        producer.sendPaymentEvent(event);

        log.info("Payment orchestration completed: paymentId={}, orderID={}",
                response.id(), response.orderId());

        return response;
    }
}
