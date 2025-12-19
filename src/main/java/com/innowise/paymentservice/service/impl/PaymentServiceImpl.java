package com.innowise.paymentservice.service.impl;

import com.innowise.paymentservice.client.RandomOrgClient;
import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.dto.PaymentSummaryResponse;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.enums.PaymentStatus;
import com.innowise.paymentservice.exception.ExternalServiceException;
import com.innowise.paymentservice.exception.PaymentAlreadyExistsException;
import com.innowise.paymentservice.exception.PaymentNotFoundException;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final RandomOrgClient randomOrgClient;

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for orderId={}, userId={}, amount={}",
                request.orderId(), request.userId(), request.paymentAmount());

        if (paymentRepository.existsByOrderId(request.orderId())) {
            log.warn("Payment already exists for orderId={}", request.orderId());
            throw new PaymentAlreadyExistsException("Payment already exists for orderId=" + request.orderId());
        }

        Payment payment = paymentMapper.toEntity(request);
        payment.setTimestamp(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PROCESSING);

        Payment savedPayment = paymentRepository.save(payment);
        log.debug("Payment saved, id={}", savedPayment.getId());

        PaymentStatus finalStatus = determinePaymentStatus();
        savedPayment.setStatus(finalStatus);

        Payment finalPayment = paymentRepository.save(savedPayment);
        log.info("Payment created successfully: id={}, status={}",
                finalPayment.getId(), finalPayment.getStatus());

        return paymentMapper.toDto(finalPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        log.debug("Getting payment for orderId={}", orderId);
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for orderId= " + orderId));

        return paymentMapper.toDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        log.debug("Getting payment for userId={}", userId);
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return paymentMapper.toDtoList(payments);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByUserId(Long userId, Pageable pageable) {
        log.debug("Getting payment for userId={} with pagination", userId);
        Page<Payment> payments = paymentRepository.findByUserId(userId, pageable);
        return payments.map(paymentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByStatus(PaymentStatus status) {
        log.debug("Getting payments with status={}", status);
        List<Payment> payments = paymentRepository.findByStatus(status);
        return paymentMapper.toDtoList(payments);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        log.debug("Getting payments for status={} with pagination", status);
        Page<Payment> payments = paymentRepository.findByStatus(status, pageable);
        return payments.map(paymentMapper::toDto);
    }

    @Override
    @Transactional
    public PaymentSummaryResponse getTotalAmountByUserAndDateRange(Long userId, LocalDateTime from, LocalDateTime to) {
        log.debug("Calculating total amount for userId={} from {} to {}", userId, from, to);

        BigDecimal totalAmount = paymentRepository.getTotalAmountByUserIdAndDateRange(userId, from, to);

        List<Payment> payments = paymentRepository.findByUserId(userId);
        long count = payments.stream()
                .filter(p -> p.getTimestamp().isAfter(from) && p.getTimestamp().isBefore(to))
                .count();

        return PaymentSummaryResponse.builder()
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .fromDate(from)
                .toDate(to)
                .userId(userId)
                .paymentsCount(count)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getTotalAmountForDateRange(LocalDateTime from, LocalDateTime to) {
        log.debug("Calculating total amount for all users from {} to {}", from, to);

        BigDecimal totalAmount = paymentRepository.getTotalAmountForDateRange(from, to);

        long count = paymentRepository.findAll().stream()
                .filter(p -> p.getTimestamp().isAfter(from) && p.getTimestamp().isBefore(to))
                .count();

        return PaymentSummaryResponse.builder()
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .fromDate(from)
                .toDate(to)
                .userId(null)
                .paymentsCount(count)
                .build();
    }

    private PaymentStatus determinePaymentStatus() {
        try {
            Integer randomNumber = randomOrgClient.generateRandomNumber();
            boolean isEven = randomOrgClient.isEven(randomNumber);

            PaymentStatus status = isEven ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            log.info("Random number: {}, status: {}", randomNumber, status);

            return status;
        } catch (RestClientException ex) {
            log.error("Failed to call Random.org API, setting payment status to FAILED: {}", ex.getMessage());
            throw new ExternalServiceException("Failed to process payment: external service unavailable", ex);
        }
    }
}
