package com.innowise.paymentservice.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.enums.PaymentStatus;
import com.innowise.paymentservice.exception.PaymentAlreadyExistsException;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@DisplayName("PaymentService Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentServiceIT extends BaseIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        resetWireMock();
    }

    @Test
    @Order(1)
    @DisplayName("should create payment with SUCCESS status when Random.org returns even number")
    void shouldCreatePayment_WithSuccessStatus_WhenEvenNumber() {
        stubRandomOrgApi("42");

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(1L)
                .userId(1L)
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.timestamp()).isNotNull();

        Payment savedPayment = paymentRepository.findByOrderId(1L).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        wireMockServer.verify(getRequestedFor(urlEqualTo("/random")));
    }

    @Test
    @Order(2)
    @DisplayName("should create payment with FAILED status when Random.org returns odd number")
    void shouldCreatePayment_WithFailedStatus_WhenOddNumber() {
        stubRandomOrgApi("13");

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(2L)
                .userId(1L)
                .paymentAmount(new BigDecimal("200.00"))
                .build();

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);

        Payment savedPayment = paymentRepository.findByOrderId(2L).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @Order(3)
    @DisplayName("should throw exception when payment already exists for order")
    void shouldThrowException_WhenPaymentAlreadyExists() {
        stubRandomOrgApi("10");

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(3L)
                .userId(1L)
                .paymentAmount(new BigDecimal("300.00"))
                .build();

        paymentService.createPayment(request);

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(PaymentAlreadyExistsException.class)
                .hasMessageContaining("Payment already exists for orderId=3");

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
    }

    @Test
    @Order(4)
    @DisplayName("should retrieve payment by orderId")
    void shouldRetrievePayment_ByOrderId() {
        stubRandomOrgApi("8");

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(4L)
                .userId(1L)
                .paymentAmount(new BigDecimal("400.00"))
                .build();

        PaymentResponse created = paymentService.createPayment(request);

        PaymentResponse retrieved = paymentService.getPaymentByOrderId(4L);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo(created.id());
        assertThat(retrieved.orderId()).isEqualTo(4L);
    }

    @Test
    @Order(5)
    @DisplayName("should retrieve all payments for user")
    void shouldRetrieveAllPayments_ForUser() {
        stubRandomOrgApi("2");
        Long userId = 1L;

        CreatePaymentRequest request1 = CreatePaymentRequest.builder()
                .orderId(5L)
                .userId(userId)
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        CreatePaymentRequest request2 = CreatePaymentRequest.builder()
                .orderId(6L)
                .userId(userId)
                .paymentAmount(new BigDecimal("200.00"))
                .build();

        paymentService.createPayment(request1);
        stubRandomOrgApi("3");
        paymentService.createPayment(request2);

        List<PaymentResponse> payments = paymentService.getPaymentsByUserId(userId);

        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(PaymentResponse::orderId)
                .containsExactlyInAnyOrder(5L, 6L);
    }

    @Test
    @Order(6)
    @DisplayName("should retrieve payments by status with pagination")
    void shouldRetrievePayments_ByStatusWithPagination() {
        for (int i = 7; i <= 11; i++) {
            stubRandomOrgApi(String.valueOf(i * 2));
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .orderId((long) i)
                    .userId(1L)
                    .paymentAmount(new BigDecimal("100.00"))
                    .build();
            paymentService.createPayment(request);
        }

        Page<PaymentResponse> page = paymentService.getPaymentsByStatus(
                PaymentStatus.SUCCESS,
                PageRequest.of(0, 3)
        );

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(p -> p.status() == PaymentStatus.SUCCESS);
    }

    @Test
    @Order(7)
    @DisplayName("should calculate total amount for user in date range")
    void shouldCalculateTotalAmount_ForUserInDateRange() {
        stubRandomOrgApi("4");
        Long userId = 2L;
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        CreatePaymentRequest request1 = CreatePaymentRequest.builder()
                .orderId(12L)
                .userId(userId)
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        CreatePaymentRequest request2 = CreatePaymentRequest.builder()
                .orderId(13L)
                .userId(userId)
                .paymentAmount(new BigDecimal("200.00"))
                .build();

        paymentService.createPayment(request1);
        stubRandomOrgApi("6");
        paymentService.createPayment(request2);

        var summary = paymentService.getTotalAmountByUserAndDateRange(userId, from, to);

        assertThat(summary).isNotNull();
        assertThat(summary.totalAmount()).isGreaterThanOrEqualTo(new BigDecimal("300.00"));
        assertThat(summary.userId()).isEqualTo(userId);
        assertThat(summary.paymentsCount()).isEqualTo(2L);
    }

    @Test
    @Order(8)
    @DisplayName("should handle Random.org API failure gracefully")
    void shouldHandleApiFailure_Gracefully() {
        stubRandomOrgApiError();

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(14L)
                .userId(1L)
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .hasMessageContaining("Failed to process payment");

        await().atMost(java.time.Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    List<Payment> payments = paymentRepository.findAll();
                    assertThat(payments).hasSize(1);
                });
    }

    private void stubRandomOrgApi(String number) {
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody(number)));
    }

    private void stubRandomOrgApiError() {
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/random"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));
    }
}