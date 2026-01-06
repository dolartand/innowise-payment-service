package com.innowise.paymentservice.security;

import com.innowise.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for authorization checks
 */
@Service("paymentSecurityService")
@RequiredArgsConstructor
@Slf4j
public class PaymentSecurityService {

    private final PaymentRepository paymentRepository;

    public boolean isPaymentOwner(Long orderId, Object userId) {
        if (userId == null) {
            log.warn("userId is null, access denied");
            return false;
        }

        try {
            Long userIdLong = userId instanceof Long ? (Long) userId : Long.parseLong(userId.toString());

            return paymentRepository.findByOrderId(orderId)
                    .map(payment -> {
                        boolean isOwner = payment.getUserId().equals(userIdLong);
                        log.debug("Payment ownership check: orderId={}, userId={}, isOwner={}",
                                orderId, userIdLong, isOwner);
                        return isOwner;
                    })
                    .orElse(false);
        } catch (NumberFormatException e) {
            log.error("Invalid user id format: {}", userId);
            return false;
        }
    }
}
