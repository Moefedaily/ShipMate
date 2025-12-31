package com.shipmate.unit.service.matching;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.shipmate.dto.response.matching.MatchResultResponse;
import com.shipmate.dto.response.matching.MatchingMetricsResponse;
import com.shipmate.mapper.MatchResultMapper;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.service.matching.ShipmentMatchingService;

@ExtendWith(MockitoExtension.class)
class ShipmentMatchingServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private MatchResultMapper matchResultMapper;

    @InjectMocks
    private ShipmentMatchingService shipmentMatchingService;

    private UUID driverId;
    private DriverProfile driverProfile;

    @BeforeEach
    void setup() {
        driverId = UUID.randomUUID();

        driverProfile = new DriverProfile();
        driverProfile.setMaxWeightCapacity(new BigDecimal("50"));
        driverProfile.setLastLatitude(new BigDecimal("48.8566"));
        driverProfile.setLastLongitude(new BigDecimal("2.3522"));
    }

    @Test
    void shouldUseLastKnownLocationWhenLatLngNotProvided() {

        Shipment shipment = new Shipment();
        shipment.setStatus(ShipmentStatus.CREATED);
        shipment.setPackageWeight(new BigDecimal("10"));
        shipment.setPickupLatitude(new BigDecimal("48.8570"));
        shipment.setPickupLongitude(new BigDecimal("2.3530"));
        shipment.setDeliveryLatitude(new BigDecimal("48.8600"));
        shipment.setDeliveryLongitude(new BigDecimal("2.3600"));

        Page<Shipment> page = new PageImpl<>(List.of(shipment));

        when(driverProfileRepository.findByUser_Id(driverId))
                .thenReturn(Optional.of(driverProfile));

        when(shipmentRepository.findByStatus(eq(ShipmentStatus.CREATED), any()))
                .thenReturn(page);

        when(matchResultMapper.toResponse(any(), any()))
                .thenReturn(MatchResultResponse.builder()
                        .metrics(MatchingMetricsResponse.builder().score(80).distanceToPickupKm(1.0).build())
                        .build());

        List<MatchResultResponse> results = shipmentMatchingService.matchShipments(
                driverId,
                null,
                null,
                null,
                10,
                10,
                PageRequest.of(0, 10)
        );

        assertThat(results).hasSize(1);
    }

    @Test
    void shouldExcludeShipmentOutsideRadius() {

        Shipment farShipment = new Shipment();
        farShipment.setStatus(ShipmentStatus.CREATED);
        farShipment.setPackageWeight(new BigDecimal("5"));
        farShipment.setPickupLatitude(new BigDecimal("50.0000"));
        farShipment.setPickupLongitude(new BigDecimal("3.0000"));
        farShipment.setDeliveryLatitude(new BigDecimal("50.1000"));
        farShipment.setDeliveryLongitude(new BigDecimal("3.1000"));

        Page<Shipment> page = new PageImpl<>(List.of(farShipment));

        when(driverProfileRepository.findByUser_Id(driverId))
                .thenReturn(Optional.of(driverProfile));

        when(shipmentRepository.findByStatus(eq(ShipmentStatus.CREATED), any()))
                .thenReturn(page);

        List<MatchResultResponse> results = shipmentMatchingService.matchShipments(
                driverId,
                null,
                null,
                null,
                1,
                10,
                PageRequest.of(0, 10)
        );

        assertThat(results).isEmpty();
    }

    @Test
    void shouldExcludeShipmentExceedingCapacity() {

        Shipment heavyShipment = new Shipment();
        heavyShipment.setStatus(ShipmentStatus.CREATED);
        heavyShipment.setPackageWeight(new BigDecimal("100"));
        heavyShipment.setPickupLatitude(new BigDecimal("48.8566"));
        heavyShipment.setPickupLongitude(new BigDecimal("2.3522"));
        heavyShipment.setDeliveryLatitude(new BigDecimal("48.8600"));
        heavyShipment.setDeliveryLongitude(new BigDecimal("2.3600"));

        Page<Shipment> page = new PageImpl<>(List.of(heavyShipment));

        when(driverProfileRepository.findByUser_Id(driverId))
                .thenReturn(Optional.of(driverProfile));

        when(shipmentRepository.findByStatus(eq(ShipmentStatus.CREATED), any()))
                .thenReturn(page);

        List<MatchResultResponse> results = shipmentMatchingService.matchShipments(
                driverId,
                null,
                null,
                null,
                50,
                10,
                PageRequest.of(0, 10)
        );

        assertThat(results).isEmpty();
    }

    @Test
    void shouldRejectMatchingWhenBookingIsInProgress() {

        User driver = new User();
        driver.setId(driverId);

        Booking booking = new Booking();
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setDriver(driver);

        when(driverProfileRepository.findByUser_Id(driverId))
                .thenReturn(Optional.of(driverProfile));

        when(bookingRepository.findWithShipmentsById(any()))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                shipmentMatchingService.matchShipments(
                        driverId,
                        null,
                        null,
                        UUID.randomUUID(),
                        10,
                        10,
                        PageRequest.of(0, 10)
                )
        ).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Booking is not eligible for matching");
    }


}