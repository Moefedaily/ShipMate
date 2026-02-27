package com.shipmate.integration.message;

import static org.assertj.core.api.Assertions.*;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.response.message.MessageResponse;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.message.Message;
import com.shipmate.model.message.MessageType;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.model.user.VehicleType;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.message.MessageRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.message.MessageService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

class MessageServiceIT extends AbstractIntegrationTest {

    @Autowired private MessageService messageService;
    @Autowired private MessageRepository messageRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DriverProfileRepository driverProfileRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void driver_shouldReadShipmentMessages() {
        TestContext ctx = prepareShipmentWithMessages();

        var page = messageService.getShipmentMessages(
                ctx.shipment.getId(),
                ctx.driver.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent())
                .extracting(MessageResponse::messageType)
                .containsExactly(
                        MessageType.SYSTEM,
                        MessageType.SYSTEM
                );
    }

    @Test
    void senderOwningShipment_shouldReadShipmentMessages() {
        TestContext ctx = prepareShipmentWithMessages();

        var page = messageService.getShipmentMessages(
                ctx.shipment.getId(),
                ctx.sender.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void messages_shouldBeOrderedBySentAtAsc() {
        TestContext ctx = prepareShipmentWithMessages();

        var page = messageService.getShipmentMessages(
                ctx.shipment.getId(),
                ctx.sender.getId(),
                PageRequest.of(0, 10)
        );

        List<MessageResponse> messages = page.getContent();
        assertThat(messages.get(0).sentAt())
                .isBefore(messages.get(1).sentAt());
    }

    @Test
    void otherUser_shouldNotReadShipmentMessages() {
        TestContext ctx = prepareShipmentWithMessages();
        User other = createUser(UserType.SENDER);

        assertThatThrownBy(() ->
                messageService.getShipmentMessages(
                        ctx.shipment.getId(),
                        other.getId(),
                        PageRequest.of(0, 10)
                )
        ).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shipmentNotFound_shouldFail() {
        User user = createUser(UserType.SENDER);

        assertThatThrownBy(() ->
                messageService.getShipmentMessages(
                        UUID.randomUUID(),
                        user.getId(),
                        PageRequest.of(0, 10)
                )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Shipment not found");
    }

    @Test
    void markMessagesAsRead_shouldMarkOnlyReceiverMessages() {
        TestContext ctx = prepareShipmentWithMessages();

        // sanity check - all messages unread
        assertThat(
            messageRepository.findByShipment_Id(ctx.shipment.getId())
                .stream()
                .allMatch(m -> !m.isRead())
        ).isTrue();

        // sender (receiver of SYSTEM messages) marks as read
        messageService.markMessagesAsRead(
                ctx.shipment.getId(),
                ctx.sender.getId()
        );

        List<Message> messages =
                messageRepository.findByShipment_Id(ctx.shipment.getId());

        assertThat(messages)
                .allMatch(Message::isRead);
    }

    @Test
    void otherUser_shouldNotMarkMessagesAsRead() {
        TestContext ctx = prepareShipmentWithMessages();
        User other = createUser(UserType.SENDER);

        assertThatThrownBy(() ->
                messageService.markMessagesAsRead(
                        ctx.shipment.getId(),
                        other.getId()
                )
        ).isInstanceOf(AccessDeniedException.class);
    }

    private TestContext prepareShipmentWithMessages() {
        User sender = createUser(UserType.SENDER);
        User driver = createUser(UserType.DRIVER);
        createDriverProfile(driver);

        Booking booking = bookingRepository.save(
                Booking.builder()
                        .driver(driver)
                        .status(BookingStatus.CONFIRMED)
                        .build()
        );

        Shipment shipment = shipmentRepository.save(
                Shipment.builder()
                        .booking(booking)
                        .sender(sender)
                        .status(ShipmentStatus.ASSIGNED)
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
                        .basePrice(BigDecimal.valueOf(20))
                        .build()
        );

        messageRepository.save(
                Message.builder()
                        .shipment(shipment)
                        .sender(driver)
                        .receiver(sender)
                        .messageType(MessageType.SYSTEM)
                        .messageContent("Booking confirmed")
                        .isRead(false)
                        .sentAt(Instant.now().minusSeconds(60))
                        .build()
        );

        messageRepository.save(
                Message.builder()
                        .shipment(shipment)
                        .sender(driver)
                        .receiver(sender)
                        .messageType(MessageType.SYSTEM)
                        .messageContent("Driver started delivery")
                        .isRead(false)
                        .sentAt(Instant.now())
                        .build()
        );

        return new TestContext(sender, driver, shipment);
    }

    private User createUser(UserType type) {
        return userRepository.save(
                User.builder()
                        .email(type.name().toLowerCase() + "-" + UUID.randomUUID() + "@shipmate.com")
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Test")
                        .lastName("User")
                        .role(Role.USER)
                        .userType(type)
                        .verified(true)
                        .active(true)
                        .build()
        );
    }

    private void createDriverProfile(User driver) {
        driverProfileRepository.save(
                DriverProfile.builder()
                        .user(driver)
                        .status(DriverStatus.APPROVED)
                        .vehicleType(VehicleType.CAR)
                        .licenseNumber("LIC-" + UUID.randomUUID())
                        .maxWeightCapacity(BigDecimal.valueOf(100))
                        .approvedAt(Instant.now())
                        .lastLatitude(BigDecimal.valueOf(48.8566))
                        .lastLongitude(BigDecimal.valueOf(2.3522))
                        .lastLocationUpdatedAt(Instant.now())
                        .build()
        );
    }

    private record TestContext(User sender, User driver, Shipment shipment) {}
}