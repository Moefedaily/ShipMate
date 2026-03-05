package com.shipmate.mapper.payment;

import com.shipmate.dto.response.admin.AdminPaymentResponse;
import com.shipmate.model.payment.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminPaymentMapper {
    @Mapping(target = "shipmentId", source = "shipment.id")
    @Mapping(target = "senderId", source = "sender.id")
    AdminPaymentResponse toResponse(Payment payment);
}