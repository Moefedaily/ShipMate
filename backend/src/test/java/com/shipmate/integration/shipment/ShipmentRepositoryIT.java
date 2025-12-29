package com.shipmate.integration.shipment;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;

class ShipmentRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldPersistShipment_withRequiredFields() {
        User user = createUser();

        Shipment shipment = Shipment.builder()
                .sender(user)
                .pickupAddress("Paris")
                .pickupLatitude(BigDecimal.valueOf(48.8566))
                .pickupLongitude(BigDecimal.valueOf(2.3522))
                .deliveryAddress("Lyon")
                .deliveryLatitude(BigDecimal.valueOf(45.7640))
                .deliveryLongitude(BigDecimal.valueOf(4.8357))
                .packageWeight(BigDecimal.valueOf(2.5))
                .packageValue(BigDecimal.valueOf(100))
                .requestedPickupDate(LocalDate.now())
                .requestedDeliveryDate(LocalDate.now().plusDays(1))
                .status(ShipmentStatus.CREATED)
                .basePrice(BigDecimal.valueOf(20))
                .extraInsuranceFee(BigDecimal.ZERO)
                .build();

        Shipment saved = shipmentRepository.saveAndFlush(shipment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSender().getId()).isEqualTo(user.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    /* =========================
       DB CONSTRAINTS
       ========================= */

    @Test
    void shouldFail_whenPackageWeightIsZero() {
        User user = createUser();

        Shipment shipment = Shipment.builder()
                .sender(user)
                .pickupAddress("Paris")
                .pickupLatitude(BigDecimal.valueOf(48.8566))
                .pickupLongitude(BigDecimal.valueOf(2.3522))
                .deliveryAddress("Lyon")
                .deliveryLatitude(BigDecimal.valueOf(45.7640))
                .deliveryLongitude(BigDecimal.valueOf(4.8357))
                .packageWeight(BigDecimal.ZERO) // invalid
                .packageValue(BigDecimal.valueOf(100))
                .requestedPickupDate(LocalDate.now())
                .requestedDeliveryDate(LocalDate.now().plusDays(1))
                .status(ShipmentStatus.CREATED)
                .basePrice(BigDecimal.valueOf(20))
                .extraInsuranceFee(BigDecimal.ZERO)
                .build();

        assertThatThrownBy(() -> shipmentRepository.saveAndFlush(shipment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFail_whenPickupDateAfterDeliveryDate() {
        User user = createUser();

        Shipment shipment = Shipment.builder()
                .sender(user)
                .pickupAddress("Paris")
                .pickupLatitude(BigDecimal.valueOf(48.8566))
                .pickupLongitude(BigDecimal.valueOf(2.3522))
                .deliveryAddress("Lyon")
                .deliveryLatitude(BigDecimal.valueOf(45.7640))
                .deliveryLongitude(BigDecimal.valueOf(4.8357))
                .packageWeight(BigDecimal.valueOf(2.5))
                .packageValue(BigDecimal.valueOf(100))
                .requestedPickupDate(LocalDate.now().plusDays(2)) // invalid
                .requestedDeliveryDate(LocalDate.now())
                .status(ShipmentStatus.CREATED)
                .basePrice(BigDecimal.valueOf(20))
                .extraInsuranceFee(BigDecimal.ZERO)
                .build();

        assertThatThrownBy(() -> shipmentRepository.saveAndFlush(shipment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /* =========================
       QUERIES
       ========================= */

    @Test
    void findBySender_shouldReturnOnlyUserShipments() {
        User u1 = createUser();
        User u2 = createUser();

        shipmentRepository.saveAndFlush(createShipment(u1));
        shipmentRepository.saveAndFlush(createShipment(u1));
        shipmentRepository.saveAndFlush(createShipment(u2));

        var page = shipmentRepository.findBySender(u1, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByIdAndSender_shouldEnforceOwnership() {
        User owner = createUser();
        User other = createUser();

        Shipment shipment = shipmentRepository.saveAndFlush(createShipment(owner));

        assertThat(shipmentRepository.findByIdAndSender(shipment.getId(), owner)).isPresent();
        assertThat(shipmentRepository.findByIdAndSender(shipment.getId(), other)).isEmpty();
    }

    /* =========================
       HELPERS
       ========================= */

    private User createUser() {
        return userRepository.save(
                User.builder()
                        .email("user-" + UUID.randomUUID() + "@shipmate.com")
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Test")
                        .lastName("User")
                        .role(Role.USER)
                        .userType(UserType.SENDER)
                        .verified(true)
                        .active(true)
                        .build()
        );
    }

    private Shipment createShipment(User user) {
        return Shipment.builder()
                .sender(user)
                .pickupAddress("Paris")
                .pickupLatitude(BigDecimal.valueOf(48.8566))
                .pickupLongitude(BigDecimal.valueOf(2.3522))
                .deliveryAddress("Lyon")
                .deliveryLatitude(BigDecimal.valueOf(45.7640))
                .deliveryLongitude(BigDecimal.valueOf(4.8357))
                .packageWeight(BigDecimal.valueOf(2.5))
                .packageValue(BigDecimal.valueOf(100))
                .requestedPickupDate(LocalDate.now())
                .requestedDeliveryDate(LocalDate.now().plusDays(1))
                .status(ShipmentStatus.CREATED)
                .basePrice(BigDecimal.valueOf(20))
                .extraInsuranceFee(BigDecimal.ZERO)
                .build();
    }
}
