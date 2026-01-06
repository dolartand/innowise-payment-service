package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.dto.PaymentSummaryResponse;
import com.innowise.paymentservice.enums.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepository;
import com.innowise.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    /**
     * Create new payment (only for ADMIN)
     * Payments are creates automatically via Kafka events
     * @param request payment data
     * @return created payment
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        log.info("Manual creation payment request for orderId={}", request.orderId());
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get payment by order id
     * @param orderId order id
     * @return payment info
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or @paymentSecurityService.isPaymentOwner(#orderId, authentication.principal)")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @PathVariable Long orderId
    ) {
        log.debug("Getting payment for orderId={}", orderId);
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all payments for user
     * @param userId user id
     * @return list of users payments
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByUserId(
            @PathVariable Long userId
    ) {
        log.debug("Getting payments for userId={}", userId);
        List<PaymentResponse> payments = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get paginated payments for user
     * @param userId user id
     * @param pageable pagination params
     * @return page of users payments
     */
    @GetMapping("/user/{userId}/paged")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsByUserIdPaged(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable
    ) {
        log.debug("Getting paginated payments for userId={}", userId);
        Page<PaymentResponse> payments = paymentService.getPaymentsByUserId(userId, pageable);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get all payments by status (only ADMIN)
     * @param status payment status
     * @return list of payments by status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(
            @PathVariable PaymentStatus status
    ) {
        log.debug("Getting payments with status={}", status);
        List<PaymentResponse> payments = paymentService.getPaymentsByStatus(status);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get paginated payments by status (only ADMIN)
     * @param status payment status
     * @param pageable pagination params
     * @return page of payments by status
     */
    @GetMapping("/status/{status}/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsByStatusPaged(
            @PathVariable PaymentStatus status,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable
    ) {
        log.debug("Getting paginated payments with status={}", status);
        Page<PaymentResponse> payments = paymentService.getPaymentsByStatus(status, pageable);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payments statistic for user with date range (only ADMIN)
     * @param userId user id
     * @param from start date
     * @param to end date
     * @return payment summary
     */
    @GetMapping("/user/{userId}/summary")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public ResponseEntity<PaymentSummaryResponse> getUserPaymentSummary(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        log.debug("Getting payment summary for userId={} from {} to {}", userId, from, to);
        PaymentSummaryResponse summary = paymentService.getTotalAmountByUserAndDateRange(userId, from, to);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get payments statistic for all users with date range (only ADMIN)
     * @param from start date
     * @param to end date
     * @return payment summary
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentSummaryResponse> getGlobalPaymentSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        log.debug("Getting global payment summary from {} to {}", from, to);
        PaymentSummaryResponse summary = paymentService.getTotalAmountForDateRange(from, to);
        return ResponseEntity.ok(summary);
    }
}
