package com.shipmate.unit.service.shipment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.cloudinary.Cloudinary;
import com.shipmate.dto.request.shipment.CreateShipmentRequest;
import com.shipmate.dto.request.shipment.UpdateShipmentRequest;
import com.shipmate.dto.response.shipment.ShipmentResponse;
import com.shipmate.mapper.shipment.ShipmentAssembler;
import com.shipmate.mapper.shipment.ShipmentMapper;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.delivery.DeliveryCodeService;
import com.shipmate.service.payment.PaymentService;
import com.shipmate.service.pricing.PricingService;
import com.shipmate.service.shipment.ShipmentService;
import com.shipmate.listener.delivery.DeliveryCodeEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ShipmentMapper shipmentMapper;
    @Mock private ShipmentAssembler shipmentAssembler;
    @Mock private Cloudinary cloudinary;
    @Mock private PricingService pricingService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentService paymentService;
    @Mock private DeliveryCodeService deliveryCodeService;
    @Mock private DeliveryCodeEventPublisher deliveryCodeEventPublisher;

    @InjectMocks
    private ShipmentService shipmentService;

    @BeforeEach
        void setupInsuranceConfig() {
        ReflectionTestUtils.setField(shipmentService, "insuranceDeductibleRate", BigDecimal.valueOf(0.10));
        ReflectionTestUtils.setField(shipmentService, "tier1Limit", BigDecimal.valueOf(100));
        ReflectionTestUtils.setField(shipmentService, "tier1Rate", BigDecimal.valueOf(0.05));
        ReflectionTestUtils.setField(shipmentService, "tier2Limit", BigDecimal.valueOf(500));
        ReflectionTestUtils.setField(shipmentService, "tier2Rate", BigDecimal.valueOf(0.10));
        ReflectionTestUtils.setField(shipmentService, "maxDeclaredValue", BigDecimal.valueOf(1000));
    }
    @Test
    void create_shouldCreateShipment_whenUserExists() {

        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();

        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setPickupAddress("Paris");
        request.setPickupLatitude(BigDecimal.valueOf(48.8566));
        request.setPickupLongitude(BigDecimal.valueOf(2.3522));
        request.setDeliveryAddress("Lyon");
        request.setDeliveryLatitude(BigDecimal.valueOf(45.7640));
        request.setDeliveryLongitude(BigDecimal.valueOf(4.8357));
        request.setPackageWeight(BigDecimal.valueOf(2.5));
        request.setPackageValue(BigDecimal.valueOf(100));
        request.setRequestedPickupDate(LocalDate.now());
        request.setRequestedDeliveryDate(LocalDate.now().plusDays(1));
        request.setInsuranceSelected(false);

        Shipment shipment = Shipment.builder().build();
        Shipment savedShipment = Shipment.builder().build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentMapper.toEntity(request)).thenReturn(shipment);
        when(pricingService.computeBasePrice(any())).thenReturn(BigDecimal.valueOf(50));
        when(shipmentRepository.saveAndFlush(shipment)).thenReturn(savedShipment);
        when(shipmentAssembler.toResponse(savedShipment)).thenReturn(new ShipmentResponse());

        ShipmentResponse response = shipmentService.create(userId, request);

        assertThat(response).isNotNull();
        verify(userRepository).findById(userId);
        verify(shipmentMapper).toEntity(request);
        verify(pricingService).computeBasePrice(any());
        verify(shipmentRepository).saveAndFlush(shipment);
        verify(shipmentAssembler).toResponse(savedShipment);
    }

    @Test
    void create_shouldFail_whenUserNotFound() {

        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                shipmentService.create(userId, new CreateShipmentRequest())
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("User not found");

        verify(shipmentRepository, never()).saveAndFlush(any());
    }


    @Test
    void update_shouldFail_whenShipmentNotCreated() {

        UUID userId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();

        User user = User.builder().id(userId).build();

        Shipment shipment = Shipment.builder()
                .id(shipmentId)
                .sender(user)
                .status(ShipmentStatus.ASSIGNED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentRepository.findByIdAndSender(shipmentId, user))
                .thenReturn(Optional.of(shipment));

        UpdateShipmentRequest request = new UpdateShipmentRequest();

        assertThatThrownBy(() ->
                shipmentService.update(shipmentId, userId, request)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("Shipment can no longer be modified");

        verify(shipmentMapper, never()).updateEntity(any(), any());
    }


    @Test
    void delete_shouldDeleteShipment_whenCreated() {

        UUID userId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();

        User user = User.builder().id(userId).build();

        Shipment shipment = Shipment.builder()
                .id(shipmentId)
                .sender(user)
                .status(ShipmentStatus.CREATED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentRepository.findByIdAndSender(shipmentId, user))
                .thenReturn(Optional.of(shipment));

        shipmentService.delete(shipmentId, userId);

        verify(shipmentRepository).delete(shipment);
    }

    @Test
    void delete_shouldFail_whenNotCreated() {

        UUID userId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();

        User user = User.builder().id(userId).build();

        Shipment shipment = Shipment.builder()
                .id(shipmentId)
                .sender(user)
                .status(ShipmentStatus.DELIVERED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentRepository.findByIdAndSender(shipmentId, user))
                .thenReturn(Optional.of(shipment));

        assertThatThrownBy(() ->
                shipmentService.delete(shipmentId, userId)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("Shipment can no longer be deleted");

        verify(shipmentRepository, never()).delete(any());
    }
    @Test
        void create_shouldFail_whenDeclaredValueExceedsPackageValue() {

        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();

        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setPickupLatitude(BigDecimal.ONE);
        request.setPickupLongitude(BigDecimal.ONE);
        request.setDeliveryLatitude(BigDecimal.TEN);
        request.setDeliveryLongitude(BigDecimal.TEN);
        request.setPackageWeight(BigDecimal.ONE);
        request.setPackageValue(BigDecimal.valueOf(100));
        request.setInsuranceSelected(true);
        request.setDeclaredValue(BigDecimal.valueOf(200)); // exceeds package value

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentMapper.toEntity(request)).thenReturn(new Shipment());
        when(pricingService.computeBasePrice(any())).thenReturn(BigDecimal.TEN);

        assertThatThrownBy(() ->
                shipmentService.create(userId, request)
        ).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Declared value cannot exceed package value");
        }

     @Test
    void create_shouldFail_whenDeclaredValueExceedsMaxAllowed() {

        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();

        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setPickupLatitude(BigDecimal.ONE);
        request.setPickupLongitude(BigDecimal.ONE);
        request.setDeliveryLatitude(BigDecimal.TEN);
        request.setDeliveryLongitude(BigDecimal.TEN);
        request.setPackageWeight(BigDecimal.ONE);
        request.setPackageValue(BigDecimal.valueOf(2000));
        request.setInsuranceSelected(true);
        request.setDeclaredValue(BigDecimal.valueOf(1500)); // exceeds maxDeclaredValue=1000

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentMapper.toEntity(request)).thenReturn(new Shipment());
        when(pricingService.computeBasePrice(any())).thenReturn(BigDecimal.TEN);

        assertThatThrownBy(() ->
                shipmentService.create(userId, request)
        ).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exceeds maximum allowed insurance limit");
    }   

    @Test
    void create_shouldApplyTier1InsuranceRate() {

        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();

        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setPickupLatitude(BigDecimal.ONE);
        request.setPickupLongitude(BigDecimal.ONE);
        request.setDeliveryLatitude(BigDecimal.TEN);
        request.setDeliveryLongitude(BigDecimal.TEN);
        request.setPackageWeight(BigDecimal.ONE);
        request.setPackageValue(BigDecimal.valueOf(100));
        request.setInsuranceSelected(true);
        request.setDeclaredValue(BigDecimal.valueOf(80));

        Shipment shipment = new Shipment();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentMapper.toEntity(request)).thenReturn(shipment);
        when(pricingService.computeBasePrice(any())).thenReturn(BigDecimal.TEN);
        when(shipmentRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shipmentAssembler.toResponse(any())).thenReturn(new ShipmentResponse());

        shipmentService.create(userId, request);

        assertThat(shipment.getInsuranceFee()).isEqualByComparingTo("4.00");
    }

    @Test
    void create_shouldApplyTier2InsuranceRate() {

        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();

        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setPickupLatitude(BigDecimal.ONE);
        request.setPickupLongitude(BigDecimal.ONE);
        request.setDeliveryLatitude(BigDecimal.TEN);
        request.setDeliveryLongitude(BigDecimal.TEN);
        request.setPackageWeight(BigDecimal.ONE);
        request.setPackageValue(BigDecimal.valueOf(500));
        request.setInsuranceSelected(true);
        request.setDeclaredValue(BigDecimal.valueOf(200));

        Shipment shipment = new Shipment();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentMapper.toEntity(request)).thenReturn(shipment);
        when(pricingService.computeBasePrice(any())).thenReturn(BigDecimal.TEN);
        when(shipmentRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shipmentAssembler.toResponse(any())).thenReturn(new ShipmentResponse());

        shipmentService.create(userId, request);

        assertThat(shipment.getInsuranceFee()).isEqualByComparingTo("20.00");
    }

   @Test
    void markInTransit_shouldFail_whenStatusNotAssigned() {

        UUID shipmentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        var driver = User.builder().id(driverId).build();

        var booking = new com.shipmate.model.booking.Booking();
        booking.setDriver(driver);
        booking.setStatus(com.shipmate.model.booking.BookingStatus.IN_PROGRESS);

        Shipment shipment = Shipment.builder()
                .id(shipmentId)
                .status(ShipmentStatus.CREATED)
                .booking(booking)
                .build();

        when(shipmentRepository.findWithBookingAndSender(shipmentId))
                .thenReturn(Optional.of(shipment));

        assertThatThrownBy(() ->
                shipmentService.markInTransit(shipmentId, driverId)
        ).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Shipment cannot move to IN_TRANSIT");
    }

    @Test
    void markInTransit_shouldFail_whenPaymentNotAuthorized() {

        UUID shipmentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        var driver = User.builder().id(driverId).build();

        var booking = new com.shipmate.model.booking.Booking();
        booking.setStatus(com.shipmate.model.booking.BookingStatus.IN_PROGRESS);
        booking.setDriver(driver);

        Shipment shipment = Shipment.builder()
                .id(shipmentId)
                .status(ShipmentStatus.ASSIGNED)
                .booking(booking)
                .build();

        var payment = com.shipmate.model.payment.Payment.builder()
                .paymentStatus(com.shipmate.model.payment.PaymentStatus.PROCESSING)
                .build();

        when(shipmentRepository.findWithBookingAndSender(shipmentId))
                .thenReturn(Optional.of(shipment));
        when(paymentRepository.findByShipment(shipment))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() ->
                shipmentService.markInTransit(shipmentId, driverId)
        ).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Shipment payment is not authorized");
    }

    @Test
    void markInTransit_shouldSucceed_whenValid() {

        UUID shipmentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        var driver = User.builder().id(driverId).build();

        var booking = new com.shipmate.model.booking.Booking();
        booking.setStatus(com.shipmate.model.booking.BookingStatus.IN_PROGRESS);
        booking.setDriver(driver);

        Shipment shipment = Shipment.builder()
                .id(shipmentId)
                .status(ShipmentStatus.ASSIGNED)
                .booking(booking)
                .build();

        var payment = com.shipmate.model.payment.Payment.builder()
                .paymentStatus(com.shipmate.model.payment.PaymentStatus.AUTHORIZED)
                .build();

        when(shipmentRepository.findWithBookingAndSender(shipmentId))
                .thenReturn(Optional.of(shipment));
        when(paymentRepository.findByShipment(shipment))
                .thenReturn(Optional.of(payment));
        when(shipmentAssembler.toResponse(shipment))
                .thenReturn(new ShipmentResponse());

        ShipmentResponse response =
                shipmentService.markInTransit(shipmentId, driverId);

        assertThat(shipment.getStatus())
                .isEqualTo(ShipmentStatus.IN_TRANSIT);

        verify(eventPublisher)
        .publishEvent(any(com.shipmate.listener.shipment.ShipmentStatusChangedEvent.class));
        assertThat(response).isNotNull();
    }
}