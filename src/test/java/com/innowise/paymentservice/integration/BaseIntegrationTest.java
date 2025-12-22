package com.innowise.paymentservice.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class BaseIntegrationTest {

    protected static final MongoDBContainer mongoDBContainer =
            new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
                    .withExposedPorts(27017);

    protected static final KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
                    .withExposedPorts(9093);

    protected static WireMockServer wireMockServer;

    static {
        mongoDBContainer.start();

        kafkaContainer.start();

        wireMockServer = new WireMockServer(
                WireMockConfiguration.options().dynamicPort()
        );
        wireMockServer.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);

        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafkaContainer::getBootstrapServers);

        registry.add("randomorg.api.url",
                () -> "http://localhost:" + wireMockServer.port() + "/random");

        registry.add("kafka.topics.order-events", () -> "test-order-events");
        registry.add("kafka.topics.payment-events", () -> "test-payment-events");

        registry.add("spring.kafka.consumer.group-id", () -> "test-payment-service-group");
    }

    protected void resetWireMock() {
        wireMockServer.resetAll();
    }
}