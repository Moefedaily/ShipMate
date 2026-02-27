package com.shipmate.mapper.payment;

import com.shipmate.dto.response.payment.PaymentResponse;
import com.shipmate.model.payment.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    
    @Mapping(target = "shipmentId", source = "shipment.id")
    @Mapping(target = "paymentStatus", source = "paymentStatus")
    @Mapping(target = "amountTotal", source = "amountTotal")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "failureReason", source = "failureReason")
    PaymentResponse toResponse(Payment payment);
}