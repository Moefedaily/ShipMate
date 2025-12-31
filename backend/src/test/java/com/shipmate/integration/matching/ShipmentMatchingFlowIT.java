package com.shipmate.integration.matching;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.response.matching.MatchResultResponse;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.model.user.VehicleType;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.matching.ShipmentMatchingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class ShipmentMatchingFlowIT extends AbstractIntegrationTest {

    @Autowired
    private ShipmentMatchingService shipmentMatchingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private BookingRepository bookingRepository;

     @BeforeEach
        void setUp() {
                bookingRepository.deleteAll();  
                shipmentRepository.deleteAll();
                driverProfileRepository.deleteAll();
                userRepository.deleteAll();
    }

    @Test
    void shouldReturnNearbyUnbookedShipments_usingLastKnownDriverLocation() {

        // --------------------
        // GIVEN a driver with last known location
        // --------------------
        User driver = userRepository.save(
                User.builder()
                        .email("driver-" + UUID.randomUUID() + "@shipmate.com")
                        .password("ignoredOne")
                        .firstName("Driver")
                        .lastName("Test")
                        .role(Role.USER)
                        .userType(UserType.DRIVER)
                        .verified(true)
                        .active(true)
                        .build()
        );

        DriverProfile profile = driverProfileRepository.save(
                DriverProfile.builder()
                        .user(driver)
                        .vehicleType(VehicleType.CAR)
                        .vehicleDescription("My car")
                        .licenseNumber("ABC-123")
                        .maxWeightCapacity(BigDecimal.valueOf(50))
                        .lastLatitude(BigDecimal.valueOf(48.8566))
                        .lastLongitude(BigDecimal.valueOf(2.3522))
                        .build()
        );

        log.info("Driver profile created: {}", profile);

        // --------------------
        // AND an available shipment nearby
        // --------------------
        User sender = userRepository.save(
                User.builder()
                        .email("sender-" + UUID.randomUUID() + "@shipmate.com")
                        .password("ignoredOne")
                        .firstName("Sender")
                        .lastName("Test")
                        .role(Role.USER)
                        .userType(UserType.SENDER)
                        .verified(true)
                        .active(true)
                        .build()
        );

        Shipment shipment = shipmentRepository.save(
                Shipment.builder()
                        .sender(sender)
                        .pickupAddress("Paris")
                        .pickupLatitude(BigDecimal.valueOf(48.8570))
                        .pickupLongitude(BigDecimal.valueOf(2.3530))
                        .deliveryAddress("Paris")
                        .deliveryLatitude(BigDecimal.valueOf(48.8600))
                        .deliveryLongitude(BigDecimal.valueOf(2.3600))
                        .packageWeight(BigDecimal.valueOf(5))
                        .packageValue(BigDecimal.valueOf(100))
                        .requestedPickupDate(LocalDate.now())
                        .requestedDeliveryDate(LocalDate.now().plusDays(1))
                        .status(ShipmentStatus.CREATED)
                        .basePrice(BigDecimal.valueOf(20))
                        .extraInsuranceFee(BigDecimal.ZERO)
                        .build()
        );

        // --------------------
        // WHEN matching is executed without explicit lat/lng
        // --------------------
        List<MatchResultResponse> results =
                shipmentMatchingService.matchShipments(
                        driver.getId(),
                        null,
                        null,
                        null,
                        10, // km radius
                        10,
                        PageRequest.of(0, 10)
                );

        // --------------------
        // THEN the shipment is matched
        // --------------------
        assertThat(results).hasSize(1);

        MatchResultResponse result = results.get(0);
        assertThat(result.getShipment().getId()).isEqualTo(shipment.getId());
        assertThat(result.getMetrics().getDistanceToPickupKm()).isNotNull();
        assertThat(result.getMetrics().getScore()).isGreaterThan(0);
    }
}
