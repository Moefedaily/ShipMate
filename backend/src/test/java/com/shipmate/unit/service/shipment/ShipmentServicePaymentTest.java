package com.shipmate.unit.service.shipment;

import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.service.payment.PaymentService;
import com.shipmate.service.shipment.ShipmentService;
import com.shipmate.mapper.shipment.ShipmentAssembler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServicePaymentTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ShipmentAssembler shipmentAssembler;
    @Mock private PaymentService paymentService;

    @InjectMocks
    private ShipmentService shipmentService;

    private UUID shipmentId;
    private UUID driverId;

    private Shipment shipment;
    private Booking booking;
    private User driver;

    @BeforeEach
    void setup() {

        shipmentId = UUID.randomUUID();
        driverId = UUID.randomUUID();

        driver = User.builder().id(driverId).build();

        booking = Booking.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .status(BookingStatus.IN_PROGRESS)
                .shipments(new ArrayList<>())
                .build();

        shipment = Shipment.builder()
                .id(shipmentId)
                .status(ShipmentStatus.ASSIGNED)
                .booking(booking)
                .build();

        booking.getShipments().add(shipment);
    }

    @Test
    void markInTransit_shouldFail_whenPaymentFailed() {

        Payment payment = Payment.builder()
                .paymentStatus(PaymentStatus.FAILED)
                .build();

        when(shipmentRepository.findWithBookingAndSender(shipmentId))
                .thenReturn(Optional.of(shipment));

        when(paymentRepository.findByShipment(shipment))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() ->
                shipmentService.markInTransit(shipmentId, driverId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markInTransit_shouldSucceed_whenPaymentAuthorized() {

        Payment payment = Payment.builder()
                .paymentStatus(PaymentStatus.AUTHORIZED)
                .build();

        when(shipmentRepository.findWithBookingAndSender(shipmentId))
                .thenReturn(Optional.of(shipment));

        when(paymentRepository.findByShipment(shipment))
                .thenReturn(Optional.of(payment));

        when(shipmentAssembler.toResponse(any()))
                .thenReturn(null);

        shipmentService.markInTransit(shipmentId, driverId);

        assertThat(shipment.getStatus())
                .isEqualTo(ShipmentStatus.IN_TRANSIT);

        verify(eventPublisher, times(1))
                .publishEvent(any(Object.class));
    }

    @Test
    void markInTransit_shouldSucceed_whenPaymentCaptured() {

        Payment payment = Payment.builder()
                .paymentStatus(PaymentStatus.CAPTURED)
                .build();

        when(shipmentRepository.findWithBookingAndSender(shipmentId))
                .thenReturn(Optional.of(shipment));

        when(paymentRepository.findByShipment(shipment))
                .thenReturn(Optional.of(payment));

        when(shipmentAssembler.toResponse(any()))
                .thenReturn(null);

        shipmentService.markInTransit(shipmentId, driverId);

        assertThat(shipment.getStatus())
                .isEqualTo(ShipmentStatus.IN_TRANSIT);

        verify(eventPublisher, times(1))
                .publishEvent(any(Object.class));
    }

    @Test
    void markDelivered_shouldCallCapture_whenShipmentInTransit() {

        shipment.setStatus(ShipmentStatus.IN_TRANSIT);

        when(shipmentRepository.findWithBookingAndSender(shipmentId))
                .thenReturn(Optional.of(shipment));

        when(shipmentAssembler.toResponse(any()))
                .thenReturn(null);

        shipmentService.markDelivered(shipmentId, driverId);

        assertThat(shipment.getStatus())
                .isEqualTo(ShipmentStatus.DELIVERED);

        verify(paymentService, times(1))
                .capturePaymentForShipment(shipment);

        verify(eventPublisher, times(2))
                .publishEvent(any(Object.class));
    }

    @Test
    void markDelivered_shouldFail_whenNotInTransit() {

        shipment.setStatus(ShipmentStatus.ASSIGNED);

        when(shipmentRepository.findWithBookingAndSender(shipmentId))
                .thenReturn(Optional.of(shipment));

        assertThatThrownBy(() ->
                shipmentService.markDelivered(shipmentId, driverId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IN_TRANSIT");
    }
}
