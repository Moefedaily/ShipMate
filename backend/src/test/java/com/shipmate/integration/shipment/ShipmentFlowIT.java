package com.shipmate.integration.shipment;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.shipment.CreateShipmentRequest;
import com.shipmate.dto.request.shipment.UpdateShipmentRequest;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.shipment.ShipmentService;

class ShipmentFlowIT extends AbstractIntegrationTest {

    @Autowired private ShipmentService shipmentService;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void create_thenUpdate_shouldSucceed_whenStatusCreated() {
        User user = createVerifiedUser("ship-flow-" + UUID.randomUUID() + "@shipmate.com");

        var created = shipmentService.create(user.getId(), createRequest());

        UpdateShipmentRequest update = new UpdateShipmentRequest();
        update.setPickupAddress("Updated Paris");
        update.setDeliveryAddress("Updated Lyon");
        update.setPackageDescription("Fragile");
        update.setPackageWeight(BigDecimal.valueOf(3));
        update.setRequestedPickupDate(LocalDate.now());
        update.setRequestedDeliveryDate(LocalDate.now().plusDays(2));

        var updated = shipmentService.update(created.getId(), user.getId(), update);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(updated.getPickupAddress()).isEqualTo("Updated Paris");
    }

    @Test
    void update_shouldFail_whenStatusNotCreated() {
        User user = createVerifiedUser("ship-update-" + UUID.randomUUID() + "@shipmate.com");

        var created = shipmentService.create(user.getId(), createRequest());

        Shipment shipment = shipmentRepository.findById(created.getId()).orElseThrow();
        shipment.setStatus(ShipmentStatus.ASSIGNED);
        shipmentRepository.saveAndFlush(shipment);

        UpdateShipmentRequest update = new UpdateShipmentRequest();
        update.setPickupAddress("X");
        update.setDeliveryAddress("Y");
        update.setPackageWeight(BigDecimal.valueOf(2));
        update.setRequestedPickupDate(LocalDate.now());
        update.setRequestedDeliveryDate(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> shipmentService.update(created.getId(), user.getId(), update))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("can no longer be modified");
    }

    @Test
    void delete_shouldSucceed_whenStatusCreated() {
        User user = createVerifiedUser("ship-del-" + UUID.randomUUID() + "@shipmate.com");

        var created = shipmentService.create(user.getId(), createRequest());

        shipmentService.delete(created.getId(), user.getId());

        assertThat(shipmentRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void delete_shouldFail_whenStatusNotCreated() {
        User user = createVerifiedUser("ship-del-fail-" + UUID.randomUUID() + "@shipmate.com");

        var created = shipmentService.create(user.getId(), createRequest());

        Shipment shipment = shipmentRepository.findById(created.getId()).orElseThrow();
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        shipmentRepository.saveAndFlush(shipment);

        assertThatThrownBy(() -> shipmentService.delete(created.getId(), user.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("can no longer be deleted");
    }

    // ---------------- helpers ----------------

    private User createVerifiedUser(String email) {
        return userRepository.save(
            User.builder()
                .email(email)
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

    private CreateShipmentRequest createRequest() {
        CreateShipmentRequest req = new CreateShipmentRequest();
        req.setPickupAddress("Paris");
        req.setPickupLatitude(BigDecimal.valueOf(48.8566));
        req.setPickupLongitude(BigDecimal.valueOf(2.3522));
        req.setDeliveryAddress("Lyon");
        req.setDeliveryLatitude(BigDecimal.valueOf(45.7640));
        req.setDeliveryLongitude(BigDecimal.valueOf(4.8357));
        req.setPackageDescription("Box");
        req.setPackageWeight(BigDecimal.valueOf(2.5));
        req.setPackageValue(BigDecimal.valueOf(100));
        req.setRequestedPickupDate(LocalDate.now());
        req.setRequestedDeliveryDate(LocalDate.now().plusDays(1));
        return req;
    }
}
