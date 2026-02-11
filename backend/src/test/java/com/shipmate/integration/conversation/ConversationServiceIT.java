package com.shipmate.integration.conversation;

import static org.assertj.core.api.Assertions.*;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.response.conversation.ConversationResponse;
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
import com.shipmate.service.conversation.ConversationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

class ConversationServiceIT extends AbstractIntegrationTest {

    @Autowired private ConversationService conversationService;

    @Autowired private BookingRepository bookingRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DriverProfileRepository driverProfileRepository;
    @Autowired private PasswordEncoder passwordEncoder;


    @Test
    void sender_shouldSeeConversation_withUnreadCount_andLastMessage() {

        TestContext ctx = prepareConversation();

        List<ConversationResponse> conversations =
                conversationService.getMyConversations(ctx.sender.getId());

        assertThat(conversations).hasSize(1);

        ConversationResponse convo = conversations.get(0);

        assertThat(convo.bookingId()).isEqualTo(ctx.booking.getId());
        assertThat(convo.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(convo.unreadCount()).isEqualTo(2);
        assertThat(convo.lastMessagePreview())
                .isEqualTo("Driver started delivery");
        assertThat(convo.lastMessageAt()).isNotNull();
    }

    @Test
    void driver_shouldSeeConversation_withUnreadCount() {

        TestContext ctx = prepareConversation();

        List<ConversationResponse> conversations =
                conversationService.getMyConversations(ctx.driver.getId());

        assertThat(conversations).hasSize(1);

        ConversationResponse convo = conversations.get(0);

        assertThat(convo.bookingId()).isEqualTo(ctx.booking.getId());
        assertThat(convo.unreadCount()).isEqualTo(0);
    }


    @Test
    void unrelatedUser_shouldNotSeeAnyConversations() {

        prepareConversation();

        User stranger = createUser(UserType.SENDER);

        List<ConversationResponse> conversations =
                conversationService.getMyConversations(stranger.getId());

        assertThat(conversations).isEmpty();
    }


    @Test
    void userWithMultipleBookings_shouldSeeMultipleConversations() {

        User sender = createUser(UserType.SENDER);
        User driver = createUser(UserType.DRIVER);
        createDriverProfile(driver);

        Booking b1 = createBookingWithMessage(sender, driver, "First booking");
        Booking b2 = createBookingWithMessage(sender, driver, "Second booking");

        List<ConversationResponse> conversations =
                conversationService.getMyConversations(sender.getId());

        assertThat(conversations).hasSize(2);

        assertThat(conversations)
                .extracting(ConversationResponse::bookingId)
                .containsExactlyInAnyOrder(b1.getId(), b2.getId());
    }


    private TestContext prepareConversation() {

        User sender = createUser(UserType.SENDER);
        User driver = createUser(UserType.DRIVER);
        createDriverProfile(driver);

        Booking booking = bookingRepository.save(
                Booking.builder()
                        .driver(driver)
                        .status(BookingStatus.CONFIRMED)
                        .build()
        );

        shipmentRepository.save(
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
                        .booking(booking)
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
                        .booking(booking)
                        .sender(driver)
                        .receiver(sender)
                        .messageType(MessageType.SYSTEM)
                        .messageContent("Driver started delivery")
                        .isRead(false)
                        .sentAt(Instant.now())
                        .build()
        );

        return new TestContext(sender, driver, booking);
    }

    private Booking createBookingWithMessage(
            User sender,
            User driver,
            String message
    ) {

        Booking booking = bookingRepository.save(
                Booking.builder()
                        .driver(driver)
                        .status(BookingStatus.CONFIRMED)
                        .build()
        );

        shipmentRepository.save(
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
                        .booking(booking)
                        .sender(driver)
                        .receiver(sender)
                        .messageType(MessageType.SYSTEM)
                        .messageContent(message)
                        .isRead(false)
                        .sentAt(Instant.now())
                        .build()
        );

        return booking;
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

    private record TestContext(User sender, User driver, Booking booking) {}
}
