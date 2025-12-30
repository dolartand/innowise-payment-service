package com.innowise.paymentservice.repository;

import com.innowise.paymentservice.dto.TotalAmountAggregationResult;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> findByOrderId(Long orderId);

    boolean existsByOrderId(Long id);

    List<Payment> findByUserId(Long userId);

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    List<Payment> findByStatus(PaymentStatus status);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    @Aggregation(pipeline = {
            "{ '$match': { 'user_id': ?0, 'timestamp': { '$gte': ?1, '$lte': ?2 } } }",
            "{ '$group': { '_id': null, 'totalAmount': { '$sum': '$payment_amount' } } }"
    })
    Optional<TotalAmountAggregationResult> getTotalAmountByUserIdAndDateRange(Long userId, Date from, Date to);

    @Aggregation(pipeline = {
            "{ '$match': { 'timestamp': { '$gte': ?0, '$lte': ?1 } } }",
            "{ '$group': { '_id': null, 'totalAmount': { '$sum': '$payment_amount' } } }"
    })
    Optional<TotalAmountAggregationResult> getTotalAmountForDateRange(Date from, Date to);
}
