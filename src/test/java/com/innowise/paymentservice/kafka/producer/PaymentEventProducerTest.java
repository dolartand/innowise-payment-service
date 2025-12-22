package com.innowise.paymentservice.kafka.producer;

import com.innowise.paymentservice.dto.PaymentEvent;
import com.innowise.paymentservice.enums.PaymentStatus;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventProducer Unit Tests")
class PaymentEventProducerTest {

    @Mock
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @InjectMocks
    private PaymentEventProducer paymentEventProducer;

    @Test
    @DisplayName("should send payment event successfully")
    void shouldSendPaymentEvent_Successfully() {
        ReflectionTestUtils.setField(paymentEventProducer, "paymentEventsTopic", "payment-events");

        PaymentEvent event = PaymentEvent.builder()
                .paymentId("payment-1")
                .orderId(1L)
                .userId(1L)
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .eventType("CREATE_PAYMENT")
                .build();

        ProducerRecord<String, PaymentEvent> producerRecord =
                new ProducerRecord<>("payment-events", "1", event);

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("payment-events", 0),
                0L, 0, System.currentTimeMillis(), 0, 0
        );

        SendResult<String, PaymentEvent> sendResult = new SendResult<>(producerRecord, metadata);
        CompletableFuture<SendResult<String, PaymentEvent>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq("payment-events"), eq("1"), eq(event))).thenReturn(future);

        paymentEventProducer.sendPaymentEvent(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("payment-events");
        assertThat(keyCaptor.getValue()).isEqualTo("1");
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }

    @Test
    @DisplayName("should handle send failure without throwing exception")
    void shouldHandleSendFailure() {
        ReflectionTestUtils.setField(paymentEventProducer, "paymentEventsTopic", "payment-events");

        PaymentEvent event = PaymentEvent.builder()
                .orderId(1L)
                .status(PaymentStatus.FAILED)
                .build();

        CompletableFuture<SendResult<String, PaymentEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka broker unavailable"));

        when(kafkaTemplate.send(eq("payment-events"), eq("1"), eq(event)))
                .thenReturn(future);

        assertThatCode(() -> paymentEventProducer.sendPaymentEvent(event))
                .doesNotThrowAnyException();

        verify(kafkaTemplate).send("payment-events", "1", event);
    }


    @Test
    @DisplayName("should use orderId as partition key")
    void shouldUseOrderIdAsPartitionKey() {
        ReflectionTestUtils.setField(paymentEventProducer, "paymentEventsTopic", "payment-events");

        PaymentEvent event = PaymentEvent.builder()
                .orderId(123L)
                .userId(1L)
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(new BigDecimal("100.00"))
                .timestamp(LocalDateTime.now())
                .build();

        ProducerRecord<String, PaymentEvent> producerRecord =
                new ProducerRecord<>("payment-events", "123", event);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("payment-events", 0),
                0L, 0, System.currentTimeMillis(), 0, 0
        );
        SendResult<String, PaymentEvent> sendResult = new SendResult<>(producerRecord, metadata);
        CompletableFuture<SendResult<String, PaymentEvent>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq("payment-events"), eq("123"), eq(event))).thenReturn(future);

        paymentEventProducer.sendPaymentEvent(event);

        verify(kafkaTemplate).send(eq("payment-events"), eq("123"), eq(event));
    }
}