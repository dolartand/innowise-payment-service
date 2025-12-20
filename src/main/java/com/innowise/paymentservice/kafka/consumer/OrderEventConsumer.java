package com.innowise.paymentservice.kafka.consumer;

import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.OrderCreatedEvent;
import com.innowise.paymentservice.dto.PaymentEvent;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.exception.PaymentAlreadyExistsException;
import com.innowise.paymentservice.kafka.producer.PaymentEventProducer;
import com.innowise.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final PaymentService paymentService;
    private final PaymentEventProducer producer;

    @KafkaListener(
            topics = "${kafka.topics.order-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreatedEvent(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("Received ORDER_CREATED event: orderId={}, userId={}, amount={}, partition={}, offset={}",
                event.orderId(), event.userId(), event.totalAmount(), partition, offset);
        try {
            CreatePaymentRequest request = event.toCreatePaymentRequest();

            PaymentResponse response = paymentService.createPayment(request);
            log.info("Payment created successfully: paymentId={}, orderId={}, status={}",
                    response.id(), response.orderId(), response.status());

            PaymentEvent paymentEvent = PaymentEvent.fromPaymentResponse(response);

            producer.sendPaymentEvent(paymentEvent);

            acknowledgment.acknowledge();
            log.info("ORDER_CREATED event processed successfully: orderId={}", event.orderId());
        } catch (PaymentAlreadyExistsException e) {
            log.warn("Payment already exists for orderId={}, skip={}", event.orderId(), e.getMessage());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing ORDER_CREATED event for orderId={}: {}",
                    event.orderId(), e.getMessage(), e);  // TODO: add DLQ
            throw new RuntimeException("Error processing ORDER_CREATED event", e);
        }
    }
}
