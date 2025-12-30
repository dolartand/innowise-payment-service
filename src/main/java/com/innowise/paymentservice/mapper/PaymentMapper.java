package com.innowise.paymentservice.mapper;

import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentResponse;
import com.innowise.paymentservice.entity.Payment;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    @Mapping(target = "paymentAmount", source = "paymentAmount")
    Payment toEntity(CreatePaymentRequest dto);

    @Mapping(target = "totalAmount", source = "paymentAmount")
    PaymentResponse toDto(Payment entity);

    List<PaymentResponse> toDtoList(List<Payment> entities);
}
