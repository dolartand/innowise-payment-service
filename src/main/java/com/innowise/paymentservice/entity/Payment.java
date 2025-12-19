package com.innowise.paymentservice.entity;

import com.innowise.paymentservice.enums.PaymentStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Payment {

    @Id
    private String id;

    @Field("order_id")
    @Indexed(unique = true)
    private Long orderId;

    @Field("user_id")
    private Long userId;

    @Field("status")
    @Indexed
    private PaymentStatus status;

    @Field("timestamp")
    @Indexed
    private LocalDateTime timestamp;

    @Field("payment_amount")
    private BigDecimal paymentAmount;
}
