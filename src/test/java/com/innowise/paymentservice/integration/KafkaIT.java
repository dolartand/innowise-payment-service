package com.innowise.paymentservice.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.paymentservice.dto.OrderCreatedEvent;
import com.innowise.paymentservice.dto.PaymentEvent;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.enums.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DisplayName("Kafka Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KafkaIT extends BaseIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${kafka.topics.order-events}")
    private String orderEventsTopic;

    @Value("${kafka.topics.payment-events}")
    private String paymentEventsTopic;

    private Producer<String, OrderCreatedEvent> orderProducer;
    private Consumer<String, PaymentEvent> paymentConsumer;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        resetWireMock();

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        orderProducer = new DefaultKafkaProducerFactory<String, OrderCreatedEvent>(producerProps)
                .createProducer();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentEvent.class.getName());

        paymentConsumer = new DefaultKafkaConsumerFactory<String, PaymentEvent>(consumerProps)
                .createConsumer();

        paymentConsumer.subscribe(Collections.singletonList(paymentEventsTopic));

        paymentConsumer.poll(Duration.ofMillis(100));
    }

    @AfterEach
    void tearDown() {
        if (orderProducer != null) {
            orderProducer.close();
        }
        if (paymentConsumer != null) {
            paymentConsumer.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("should process ORDER_CREATED event and publish PAYMENT_CREATED event with SUCCESS")
    void shouldProcessOrderCreatedEvent_AndPublishPaymentEvent_WithSuccess() {
        stubRandomOrgApi("42");

        OrderCreatedEvent orderEvent = OrderCreatedEvent.builder()
                .orderId(1L)
                .userId(1L)
                .totalAmount(new BigDecimal("100.00"))
                .event("ORDER_CREATED")
                .build();

        ProducerRecord<String, OrderCreatedEvent> record =
                new ProducerRecord<>(orderEventsTopic, "1", orderEvent);
        orderProducer.send(record);
        orderProducer.flush();

        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Payment payment = paymentRepository.findByOrderId(1L).orElse(null);
                    assertThat(payment).isNotNull();
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
                    assertThat(payment.getUserId()).isEqualTo(1L);
                    assertThat(payment.getPaymentAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
                });

        PaymentEvent paymentEvent = pollForPaymentEvent(Duration.ofSeconds(10));

        assertThat(paymentEvent).isNotNull();
        assertThat(paymentEvent.orderId()).isEqualTo(1L);
        assertThat(paymentEvent.userId()).isEqualTo(1L);
        assertThat(paymentEvent.status()).isEqualTo(PaymentStatus.SUCCESS);
        if (paymentEvent.paymentAmount() != null) {
            assertThat(paymentEvent.paymentAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }
        assertThat(paymentEvent.eventType()).isEqualTo("CREATE_PAYMENT");
    }

    @Test
    @Order(2)
    @DisplayName("should process ORDER_CREATED event and publish PAYMENT_CREATED event with FAILED")
    void shouldProcessOrderCreatedEvent_AndPublishPaymentEvent_WithFailed() {
        stubRandomOrgApi("13");

        OrderCreatedEvent orderEvent = OrderCreatedEvent.builder()
                .orderId(2L)
                .userId(2L)
                .totalAmount(new BigDecimal("200.00"))
                .event("ORDER_CREATED")
                .build();

        ProducerRecord<String, OrderCreatedEvent> record =
                new ProducerRecord<>(orderEventsTopic, "2", orderEvent);
        orderProducer.send(record);
        orderProducer.flush();

        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Payment payment = paymentRepository.findByOrderId(2L).orElse(null);
                    assertThat(payment).isNotNull();
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
                });

        PaymentEvent paymentEvent = pollForPaymentEvent(Duration.ofSeconds(10));

        assertThat(paymentEvent).isNotNull();
        assertThat(paymentEvent.orderId()).isEqualTo(2L);
        assertThat(paymentEvent.status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @Order(3)
    @DisplayName("should handle duplicate ORDER_CREATED events gracefully")
    void shouldHandleDuplicateOrderEvents_Gracefully() {
        stubRandomOrgApi("10");

        OrderCreatedEvent orderEvent = OrderCreatedEvent.builder()
                .orderId(3L)
                .userId(3L)
                .totalAmount(new BigDecimal("300.00"))
                .event("ORDER_CREATED")
                .build();

        ProducerRecord<String, OrderCreatedEvent> record1 =
                new ProducerRecord<>(orderEventsTopic, "3", orderEvent);
        ProducerRecord<String, OrderCreatedEvent> record2 =
                new ProducerRecord<>(orderEventsTopic, "3", orderEvent);

        orderProducer.send(record1);
        orderProducer.send(record2);
        orderProducer.flush();

        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    long count = paymentRepository.count();
                    assertThat(count).isEqualTo(1L);
                });

        Payment payment = paymentRepository.findByOrderId(3L).orElseThrow();
        assertThat(payment).isNotNull();

        PaymentEvent paymentEvent = pollForPaymentEvent(Duration.ofSeconds(10));
        assertThat(paymentEvent).isNotNull();
        assertThat(paymentEvent.orderId()).isEqualTo(3L);

        paymentConsumer.poll(Duration.ofMillis(100));
    }

    @Test
    @Order(4)
    @DisplayName("should process multiple ORDER_CREATED events in order")
    void shouldProcessMultipleOrderEvents_InOrder() {
        OrderCreatedEvent event1 = OrderCreatedEvent.builder()
                .orderId(4L).userId(4L).totalAmount(new BigDecimal("100.00"))
                .event("ORDER_CREATED").build();

        OrderCreatedEvent event2 = OrderCreatedEvent.builder()
                .orderId(5L).userId(4L).totalAmount(new BigDecimal("200.00"))
                .event("ORDER_CREATED").build();

        OrderCreatedEvent event3 = OrderCreatedEvent.builder()
                .orderId(6L).userId(4L).totalAmount(new BigDecimal("300.00"))
                .event("ORDER_CREATED").build();

        stubRandomOrgApi("2");
        orderProducer.send(new ProducerRecord<>(orderEventsTopic, "4", event1));

        stubRandomOrgApi("4");
        orderProducer.send(new ProducerRecord<>(orderEventsTopic, "5", event2));

        stubRandomOrgApi("6");
        orderProducer.send(new ProducerRecord<>(orderEventsTopic, "6", event3));

        orderProducer.flush();

        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    long count = paymentRepository.count();
                    assertThat(count).isEqualTo(3L);
                });

        Payment payment1 = paymentRepository.findByOrderId(4L).orElseThrow();
        Payment payment2 = paymentRepository.findByOrderId(5L).orElseThrow();
        Payment payment3 = paymentRepository.findByOrderId(6L).orElseThrow();

        assertThat(payment1.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment2.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment3.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        List<PaymentEvent> events = pollForMultiplePaymentEvents(3, Duration.ofSeconds(10));
        assertThat(events).hasSize(3);
        assertThat(events).extracting(PaymentEvent::orderId)
                .containsExactlyInAnyOrder(4L, 5L, 6L);
    }

    private PaymentEvent pollForPaymentEvent(Duration timeout) {
        long endTime = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < endTime) {
            ConsumerRecords<String, PaymentEvent> records =
                    paymentConsumer.poll(Duration.ofMillis(500));

            if (!records.isEmpty()) {
                ConsumerRecord<String, PaymentEvent> record = records.iterator().next();
                System.out.println("✅ Received PaymentEvent: " + record.value());
                return record.value();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.err.println("❌ Timeout waiting for PaymentEvent");
        return null;
    }

    private List<PaymentEvent> pollForMultiplePaymentEvents(int expectedCount, Duration timeout) {
        List<PaymentEvent> events = new ArrayList<>();
        long endTime = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < endTime && events.size() < expectedCount) {
            ConsumerRecords<String, PaymentEvent> records =
                    paymentConsumer.poll(Duration.ofMillis(500));

            for (ConsumerRecord<String, PaymentEvent> record : records) {
                events.add(record.value());
                System.out.println("Received PaymentEvent " + events.size() + "/" + expectedCount + ": " + record.value());

                if (events.size() >= expectedCount) {
                    break;
                }
            }

            if (events.size() < expectedCount) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (events.size() < expectedCount) {
            System.err.println("❌ Expected " + expectedCount + " events, but got only " + events.size());
        }

        return events;
    }

    private void stubRandomOrgApi(String number) {
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody(number)));
    }
}