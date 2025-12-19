package com.innowise.paymentservice.service;

import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.dto.PaymentSummaryResponse;
import com.innowise.paymentservice.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest createPaymentRequest);

    PaymentResponse getPaymentByOrderId(Long orderId);

    List<PaymentResponse> getPaymentsByUserId(Long userId);

    Page<PaymentResponse> getPaymentsByUserId(Long userId, Pageable pageable);

    List<PaymentResponse> getPaymentsByStatus(PaymentStatus paymentStatus);

    Page<PaymentResponse> getPaymentsByStatus(PaymentStatus paymentStatus, Pageable pageable);

    PaymentSummaryResponse getTotalAmountByUserAndDateRange(Long userId, LocalDateTime from, LocalDateTime to);

    PaymentSummaryResponse getTotalAmountForDateRange(LocalDateTime from, LocalDateTime to);
}
