package com.shipmate.integration.ws;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.dto.request.shipment.CreateShipmentRequest;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.DriverProfile.DriverStatus;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.message.MessageType;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.model.user.VehicleType;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.message.MessageRepository;
import com.shipmate.repository.notification.NotificationRepository;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.booking.BookingService;
import com.shipmate.service.shipment.ShipmentService;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BookingEventSuccessFlowIT extends AbstractIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private ShipmentService shipmentService;

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DriverProfileRepository driverProfileRepository;
    @Autowired private PasswordEncoder passwordEncoder;


    @Test
    void confirm_shouldCreateNotificationAndSystemMessage() {
        var ctx = prepareBooking();

        long notifBefore = notificationRepository.count();
        long msgBefore = systemMessageCount();

        Booking confirmed =
                bookingService.confirm(ctx.booking().getId(), ctx.driver().getId());

        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(notificationRepository.count()).isGreaterThan(notifBefore);
        assertThat(systemMessageCount()).isGreaterThan(msgBefore);
    }


    @Test
    void start_shouldCreateNotificationAndSystemMessage() {
        var ctx = prepareBooking();

        bookingService.confirm(ctx.booking().getId(), ctx.driver().getId());

        initializeAuthorizedPayment(ctx);

        long notifBefore = notificationRepository.count();
        long msgBefore = systemMessageCount();

        Booking started =
        bookingService.start(ctx.booking().getId(), ctx.driver().getId());

        assertThat(started.getStatus()).isEqualTo(BookingStatus.IN_PROGRESS);

        assertThat(notificationRepository.count())
                .isGreaterThan(notifBefore);

        assertThat(systemMessageCount())
                .isGreaterThan(msgBefore);
    }

    @Test
    void complete_shouldOnlyUpdateStatusAndPublishWsEvent() {
        var ctx = prepareBooking();

        bookingService.confirm(ctx.booking().getId(), ctx.driver().getId());
        initializeAuthorizedPayment(ctx);
        bookingService.start(ctx.booking().getId(), ctx.driver().getId());

        long notifBefore = notificationRepository.count();
        long msgBefore = systemMessageCount();

        Booking completed =
                bookingService.complete(ctx.booking().getId(), ctx.driver().getId());

        assertThat(completed.getStatus()).isEqualTo(BookingStatus.COMPLETED);

        assertThat(notificationRepository.count())
                .isEqualTo(notifBefore);

        assertThat(systemMessageCount())
                .isEqualTo(msgBefore);
    }

    @Test
    void cancel_shouldCreateNotificationAndSystemMessage() {
        var ctx = prepareBooking();

        long notifBefore = notificationRepository.count();
        long msgBefore = systemMessageCount();

        Booking cancelled =
                bookingService.cancel(ctx.booking().getId(), ctx.driver().getId());

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(notificationRepository.count()).isEqualTo(notifBefore + 1);
        assertThat(systemMessageCount()).isEqualTo(msgBefore + 1);
    }


    private long systemMessageCount() {
        return messageRepository.countByMessageType(MessageType.SYSTEM);
    }

    private void initializeAuthorizedPayment(TestContext ctx) {

        var shipment = ctx.booking().getShipments().get(0);

        paymentRepository.saveAndFlush(
                Payment.builder()
                        .shipment(shipment)
                        .sender(ctx.sender())
                        .amountTotal(BigDecimal.valueOf(100))
                        .currency("EUR")
                        .paymentStatus(PaymentStatus.AUTHORIZED)
                        .stripePaymentIntentId("pi_test_" + UUID.randomUUID())
                        .build()
        );
    }

    private TestContext prepareBooking() {
        User sender = createSender();
        User driver = createDriver();

        var shipment = shipmentService.create(sender.getId(), createShipmentRequest());

        Booking booking = bookingService.createBooking(
                driver.getId(),
                new com.shipmate.dto.request.booking.CreateBookingRequest(
                        List.of(shipment.getId())
                )
        );

        return new TestContext(sender, driver, booking);
    }

    private User createSender() {
        return userRepository.saveAndFlush(
                User.builder()
                        .email("sender-" + UUID.randomUUID() + "@shipmate.com")
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Sender")
                        .lastName("Test")
                        .role(Role.USER)
                        .userType(UserType.SENDER)
                        .verified(true)
                        .active(true)
                        .build()
        );
    }

    private User createDriver() {
        User driver = userRepository.saveAndFlush(
                User.builder()
                        .email("driver-" + UUID.randomUUID() + "@shipmate.com")
                        .password(passwordEncoder.encode("Password123!"))
                        .firstName("Driver")
                        .lastName("Test")
                        .role(Role.USER)
                        .userType(UserType.DRIVER)
                        .verified(true)
                        .active(true)
                        .build()
        );

        driverProfileRepository.saveAndFlush(
                DriverProfile.builder()
                        .user(driver)
                        .status(DriverStatus.APPROVED)
                        .vehicleType(VehicleType.TRUCK)
                        .licenseNumber("TEST-" + UUID.randomUUID())
                        .maxWeightCapacity(BigDecimal.valueOf(100))
                        .approvedAt(Instant.now())
                        .lastLatitude(BigDecimal.valueOf(48.8566))
                        .lastLongitude(BigDecimal.valueOf(2.3522))
                        .lastLocationUpdatedAt(Instant.now())
                        .build()
        );

        return driver;
    }

    private CreateShipmentRequest createShipmentRequest() {
        CreateShipmentRequest req = new CreateShipmentRequest();
        req.setPickupAddress("Paris");
        req.setPickupLatitude(BigDecimal.valueOf(48.8566));
        req.setPickupLongitude(BigDecimal.valueOf(2.3522));
        req.setDeliveryAddress("Lyon");
        req.setDeliveryLatitude(BigDecimal.valueOf(48.8049));
        req.setDeliveryLongitude(BigDecimal.valueOf(2.1204));
        req.setPackageWeight(BigDecimal.valueOf(2.5));
        req.setPackageValue(BigDecimal.valueOf(100));
        req.setRequestedPickupDate(LocalDate.now());
        req.setRequestedDeliveryDate(LocalDate.now().plusDays(1));
        return req;
    }

    private record TestContext(User sender, User driver, Booking booking) {}
}