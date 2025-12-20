package com.innowise.paymentservice.kafka.producer;

import com.innowise.paymentservice.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${kafka.topics.payment-events}")
    private String paymentEventsTopic;

    public void sendPaymentEvent(PaymentEvent event) {
        log.info("Sending payment event to Kafka: orderId={}, status={}", event.orderId(), event.status());

        try {
            String key = event.orderId().toString();

            CompletableFuture<SendResult<String, PaymentEvent>> future =
                    kafkaTemplate.send(paymentEventsTopic, key, event);

            future.whenComplete((r, e) -> {
                if (e != null) {
                    log.error("Error while sending payment event to Kafka for orderId={}: {}", event.orderId() ,e.getMessage());
                } else {
                    log.info("Payment event sent successfully: orderId={}, partition={}, offset={}",
                            event.orderId(),
                            r.getRecordMetadata().partition(),
                            r.getRecordMetadata().offset()
                    );
                }
            });
        } catch (Exception e) {
            log.error("Error sending event for orderId={}: {}", event.orderId(), e.getMessage());
            throw new RuntimeException("Failed to send payment event", e);
        }
    }
}
