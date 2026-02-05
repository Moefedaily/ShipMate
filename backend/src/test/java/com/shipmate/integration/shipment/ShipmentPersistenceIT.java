package com.shipmate.integration.shipment;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.shipment.CreateShipmentRequest;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.shipment.ShipmentService;

class ShipmentPersistenceIT extends AbstractIntegrationTest {

    @Autowired private ShipmentService shipmentService;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void create_shouldPersistDefaults_andLinkSender() {
        User user = createVerifiedUser("persist-" + UUID.randomUUID() + "@shipmate.com");

        var created = shipmentService.create(user.getId(), createRequest());

        var stored = shipmentRepository.findById(created.getId()).orElseThrow();

        assertThat(stored.getSender().getId()).isEqualTo(user.getId());
        assertThat(stored.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(stored.getExtraInsuranceFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stored.getCreatedAt()).isNotNull();
    }

    @Test
    void findByIdAndSender_shouldNotReturnOtherUsersShipment() {
        User owner = createVerifiedUser("owner-" + UUID.randomUUID() + "@shipmate.com");
        User other = createVerifiedUser("other-" + UUID.randomUUID() + "@shipmate.com");

        var created = shipmentService.create(owner.getId(), createRequest());

        assertThat(shipmentRepository.findByIdAndSender(created.getId(), other)).isEmpty();
        assertThat(shipmentRepository.findByIdAndSender(created.getId(), owner)).isPresent();
    }

    @Test
    void findBySender_shouldReturnOnlyMyShipments() {
        User u1 = createVerifiedUser("u1-" + UUID.randomUUID() + "@shipmate.com");
        User u2 = createVerifiedUser("u2-" + UUID.randomUUID() + "@shipmate.com");

        shipmentService.create(u1.getId(), createRequest());
        shipmentService.create(u1.getId(), createRequest());
        shipmentService.create(u2.getId(), createRequest());

        var page = shipmentRepository.findBySender(u1, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
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
        req.setPackageWeight(BigDecimal.valueOf(2.5));
        req.setPackageValue(BigDecimal.valueOf(100));
        req.setRequestedPickupDate(LocalDate.now());
        req.setRequestedDeliveryDate(LocalDate.now().plusDays(1));
        return req;
    }
}
