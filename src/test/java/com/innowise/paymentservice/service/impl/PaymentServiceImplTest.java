package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.RandomOrgClient;
import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.dto.PaymentSummaryResponse;
import com.innowise.paymentservice.dto.TotalAmountAggregationResult;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.enums.PaymentStatus;
import com.innowise.paymentservice.exception.ExternalServiceException;
import com.innowise.paymentservice.exception.PaymentAlreadyExistsException;
import com.innowise.paymentservice.exception.PaymentNotFoundException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Service unit tests")
public class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RandomOrgClient randomOrgClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Nested
    @DisplayName("Create payment tests")
    class CreatePaymentTests {

        @Test
        @DisplayName("should successfully create payment with status SUCCESS when random number is even")
        void shouldCreatePayment_WithSuccessStatus_WhenRandomNumberIsEven() {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .orderId(1L)
                    .userId(1L)
                    .paymentAmount(new BigDecimal("100.00"))
                    .build();

            Payment payment = Payment.builder()
                    .orderId(1L)
                    .userId(1L)
                    .paymentAmount(new BigDecimal("100.00"))
                    .build();
            
            Payment savedPayment = Payment.builder()
                    .id("payment-1")
                    .orderId(1L)
                    .userId(1L)
                    .status(PaymentStatus.SUCCESS)
                    .timestamp(LocalDateTime.now())
                    .paymentAmount(new BigDecimal("100.00"))
                    .build();

            PaymentResponse expected = PaymentResponse.builder()
                    .id("payment-1")
                    .orderId(1L)
                    .userId(1L)
                    .status(PaymentStatus.SUCCESS)
                    .timestamp(LocalDateTime.now())
                    .totalAmount(new BigDecimal("100.00"))
                    .build();

            when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
            when(paymentMapper.toEntity(request)).thenReturn(payment);
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
            when(randomOrgClient.generateRandomNumber()).thenReturn(42);
            when(randomOrgClient.isEven(42)).thenReturn(true);
            when(paymentMapper.toDto(savedPayment)).thenReturn(expected);

            PaymentResponse result = paymentService.createPayment(request);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(result.orderId()).isEqualTo(1L);

            verify(paymentRepository).existsByOrderId(1L);
            verify(paymentRepository, times(2)).save(any(Payment.class));
            verify(randomOrgClient).generateRandomNumber();
            verify(randomOrgClient).isEven(42);
        }

        @Test
        @DisplayName("should create payment with FAILED status when random number is odd")
        void shouldCreatePayment_WithFailedStatus_WhenRandomNumberIsOdd() {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .orderId(2L)
                    .userId(1L)
                    .paymentAmount(new BigDecimal("200.00"))
                    .build();

            Payment payment = Payment.builder()
                    .orderId(2L)
                    .userId(1L)
                    .paymentAmount(new BigDecimal("200.00"))
                    .build();

            Payment savedPayment = Payment.builder()
                    .id("payment-2")
                    .orderId(2L)
                    .userId(1L)
                    .paymentAmount(new BigDecimal("200.00"))
                    .status(PaymentStatus.FAILED)
                    .timestamp(LocalDateTime.now())
                    .build();

            PaymentResponse expected = PaymentResponse.builder()
                    .id("payment-2")
                    .orderId(2L)
                    .userId(1L)
                    .status(PaymentStatus.FAILED)
                    .totalAmount(new BigDecimal("200.00"))
                    .timestamp(LocalDateTime.now())
                    .build();

            when(paymentRepository.existsByOrderId(2L)).thenReturn(false);
            when(paymentMapper.toEntity(request)).thenReturn(payment);
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
            when(randomOrgClient.generateRandomNumber()).thenReturn(13); // Odd number
            when(randomOrgClient.isEven(13)).thenReturn(false);
            when(paymentMapper.toDto(savedPayment)).thenReturn(expected);

            PaymentResponse result = paymentService.createPayment(request);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
            assertThat(result.orderId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("should throw PaymentAlreadyExistsException when payment exists for order")
        void shouldThrowException_WhenPaymentAlreadyExists() {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .orderId(1L)
                    .userId(1L)
                    .paymentAmount(new BigDecimal("100.00"))
                    .build();

            when(paymentRepository.existsByOrderId(1L)).thenReturn(true);

            assertThatThrownBy(() -> paymentService.createPayment(request))
                    .isInstanceOf(PaymentAlreadyExistsException.class)
                    .hasMessageContaining("Payment already exists for orderId=1");

            verify(paymentRepository).existsByOrderId(1L);
            verify(paymentRepository, never()).save(any());
            verify(randomOrgClient, never()).generateRandomNumber();
        }

        @Test
        @DisplayName("should throw ExternalServiceException when Random.org API fails")
        void shouldThrowException_WhenRandomOrgApiFails() {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .orderId(3L)
                    .userId(1L)
                    .paymentAmount(new BigDecimal("300.00"))
                    .build();

            Payment payment = Payment.builder()
                    .orderId(3L)
                    .userId(1L)
                    .paymentAmount(new BigDecimal("300.00"))
                    .build();

            when(paymentRepository.existsByOrderId(3L)).thenReturn(false);
            when(paymentMapper.toEntity(request)).thenReturn(payment);
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
            when(randomOrgClient.generateRandomNumber())
                    .thenThrow(new RestClientException("API unavailable"));

            assertThatThrownBy(() -> paymentService.createPayment(request))
                    .isInstanceOf(ExternalServiceException.class)
                    .hasMessageContaining("Failed to process payment");

            verify(randomOrgClient).generateRandomNumber();
        }

        @Test
        @DisplayName("should set status to PROCESSING initially")
        void shouldSetStatusToProcessing_Initially() {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .orderId(4L)
                    .userId(1L)
                    .paymentAmount(new BigDecimal("400.00"))
                    .build();

            Payment payment = Payment.builder()
                    .orderId(4L)
                    .userId(1L)
                    .paymentAmount(new BigDecimal("400.00"))
                    .build();

            when(paymentRepository.existsByOrderId(4L)).thenReturn(false);
            when(paymentMapper.toEntity(request)).thenReturn(payment);
            when(paymentRepository.save(any(Payment.class)))
                    .thenAnswer(invocation -> {
                        Payment p = invocation.getArgument(0);
                        return Payment.builder()
                                .orderId(p.getOrderId())
                                .userId(p.getUserId())
                                .paymentAmount(p.getPaymentAmount())
                                .status(p.getStatus())
                                .timestamp(p.getTimestamp())
                                .build();
                    });
            when(randomOrgClient.generateRandomNumber()).thenReturn(10);
            when(randomOrgClient.isEven(10)).thenReturn(true);
            when(paymentMapper.toDto(any())).thenReturn(
                    PaymentResponse.builder().build()
            );

            paymentService.createPayment(request);

            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository, times(2)).save(paymentCaptor.capture());

            List<Payment> savedPayments = paymentCaptor.getAllValues();
            assertThat(savedPayments.getFirst().getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        }
    }

    @Nested
    @DisplayName("getPaymentByOrderId tests")
    class GetPaymentByOrderIdTests {

        @Test
        @DisplayName("should return payment when exists")
        void shouldReturnPayment_WhenExists() {
            Payment payment = Payment.builder()
                    .id("payment-1")
                    .orderId(1L)
                    .userId(1L)
                    .status(PaymentStatus.SUCCESS)
                    .paymentAmount(new BigDecimal("100.00"))
                    .timestamp(LocalDateTime.now())
                    .build();

            PaymentResponse expectedResponse = PaymentResponse.builder()
                    .id("payment-1")
                    .orderId(1L)
                    .userId(1L)
                    .status(PaymentStatus.SUCCESS)
                    .totalAmount(new BigDecimal("100.00"))
                    .timestamp(LocalDateTime.now())
                    .build();

            when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));
            when(paymentMapper.toDto(payment)).thenReturn(expectedResponse);

            PaymentResponse result = paymentService.getPaymentByOrderId(1L);

            assertThat(result).isNotNull();
            assertThat(result.orderId()).isEqualTo(1L);
            verify(paymentRepository).findByOrderId(1L);
        }

        @Test
        @DisplayName("should throw PaymentNotFoundException when not exists")
        void shouldThrowException_WhenNotExists() {
            when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentByOrderId(999L))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining("Payment not found for orderId=999");
        }
    }

    @Nested
    @DisplayName("getPaymentsByUserId tests")
    class GetPaymentsByUserIdTests {

        @Test
        @DisplayName("should return list of payments for user")
        void shouldReturnListOfPayments_ForUser() {
            List<Payment> payments = List.of(
                    Payment.builder()
                            .id("payment-1")
                            .orderId(1L)
                            .userId(1L)
                            .status(PaymentStatus.SUCCESS)
                            .paymentAmount(new BigDecimal("100.00"))
                            .build(),
                    Payment.builder()
                            .id("payment-2")
                            .orderId(2L)
                            .userId(1L)
                            .status(PaymentStatus.FAILED)
                            .paymentAmount(new BigDecimal("200.00"))
                            .build()
            );

            List<PaymentResponse> expectedResponses = List.of(
                    PaymentResponse.builder().id("payment-1").orderId(1L).build(),
                    PaymentResponse.builder().id("payment-2").orderId(2L).build()
            );

            when(paymentRepository.findByUserId(1L)).thenReturn(payments);
            when(paymentMapper.toDtoList(payments)).thenReturn(expectedResponses);

            List<PaymentResponse> result = paymentService.getPaymentsByUserId(1L);

            assertThat(result).hasSize(2);
            verify(paymentRepository).findByUserId(1L);
        }

        @Test
        @DisplayName("should return page of payments with pagination")
        void shouldReturnPaymentsPage_WithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            List<Payment> payments = List.of(
                    Payment.builder().id("payment-1").orderId(1L).userId(1L).build()
            );
            Page<Payment> paymentPage = new PageImpl<>(payments, pageable, 1);

            PaymentResponse response = PaymentResponse.builder()
                    .id("payment-1")
                    .orderId(1L)
                    .build();

            when(paymentRepository.findByUserId(1L, pageable)).thenReturn(paymentPage);
            when(paymentMapper.toDto(any(Payment.class))).thenReturn(response);

            Page<PaymentResponse> result = paymentService.getPaymentsByUserId(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getPaymentsByStatus tests")
    class GetPaymentsByStatusTests {

        @Test
        @DisplayName("should return payments by status")
        void shouldReturnPayments_ByStatus() {
            List<Payment> payments = List.of(
                    Payment.builder()
                            .id("payment-1")
                            .status(PaymentStatus.SUCCESS)
                            .build()
            );

            List<PaymentResponse> expectedResponses = List.of(
                    PaymentResponse.builder().id("payment-1").status(PaymentStatus.SUCCESS).build()
            );

            when(paymentRepository.findByStatus(PaymentStatus.SUCCESS)).thenReturn(payments);
            when(paymentMapper.toDtoList(payments)).thenReturn(expectedResponses);

            List<PaymentResponse> result = paymentService.getPaymentsByStatus(PaymentStatus.SUCCESS);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().status()).isEqualTo(PaymentStatus.SUCCESS);
        }
    }

    @Nested
    @DisplayName("getTotalAmountByUserAndDateRange tests")
    class GetTotalAmountByUserAndDateRangeTests {
        @Test
        @DisplayName("should calculate total amount for user in date range")
        void shouldCalculateTotalAmount_ForUserInDateRange() {
            Long userId = 1L;
            LocalDateTime from = LocalDateTime.now().minusDays(7);
            LocalDateTime to = LocalDateTime.now();
            BigDecimal expectedTotal = new BigDecimal("500.00");

            List<Payment> payments = List.of(
                    Payment.builder()
                            .userId(1L)
                            .timestamp(LocalDateTime.now().minusDays(3))
                            .paymentAmount(new BigDecimal("200.00"))
                            .build(),
                    Payment.builder()
                            .userId(1L)
                            .timestamp(LocalDateTime.now().minusDays(1))
                            .paymentAmount(new BigDecimal("300.00"))
                            .build()
            );

            when(paymentRepository.getTotalAmountByUserIdAndDateRange(userId, from, to))
                    .thenReturn(Optional.of(
                            new TotalAmountAggregationResult(expectedTotal)
                    ));

            when(paymentRepository.findByUserId(userId)).thenReturn(payments);

            PaymentSummaryResponse result = paymentService.getTotalAmountByUserAndDateRange(
                    userId, from, to
            );

            assertThat(result).isNotNull();
            assertThat(result.totalAmount()).isEqualByComparingTo(expectedTotal);
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.paymentsCount()).isEqualTo(2L);
        }

        @Test
        @DisplayName("should return zero when no payments in date range")
        void shouldReturnZero_WhenNoPaymentsInDateRange() {
            Long userId = 1L;
            LocalDateTime from = LocalDateTime.now().minusDays(7);
            LocalDateTime to = LocalDateTime.now();

            when(paymentRepository.getTotalAmountByUserIdAndDateRange(userId, from, to))
                    .thenReturn(Optional.empty());
            when(paymentRepository.findByUserId(userId)).thenReturn(List.of());

            PaymentSummaryResponse result = paymentService.getTotalAmountByUserAndDateRange(
                    userId, from, to
            );

            assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.paymentsCount()).isZero();
        }
    }

    @Nested
    @DisplayName("getTotalAmountForDateRange tests")
    class GetTotalAmountForAllUsersTests {

        @Test
        @DisplayName("should calculate total amount for all users")
        void shouldCalculateTotalAmount_ForAllUsers() {
            LocalDateTime from = LocalDateTime.now().minusDays(30);
            LocalDateTime to = LocalDateTime.now();
            BigDecimal expectedTotal = new BigDecimal("10000.00");

            List<Payment> allPayments = List.of(
                    Payment.builder()
                            .timestamp(LocalDateTime.now().minusDays(15))
                            .build(),
                    Payment.builder()
                            .timestamp(LocalDateTime.now().minusDays(10))
                            .build()
            );

            when(paymentRepository.getTotalAmountForDateRange(from, to))
                    .thenReturn(Optional.of(
                            new TotalAmountAggregationResult(expectedTotal)
                    ));
            when(paymentRepository.findAll()).thenReturn(allPayments);

            // When
            PaymentSummaryResponse result = paymentService.getTotalAmountForDateRange(from, to);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totalAmount()).isEqualByComparingTo(expectedTotal);
            assertThat(result.userId()).isNull();
            assertThat(result.paymentsCount()).isEqualTo(2L);
        }
    }
}
