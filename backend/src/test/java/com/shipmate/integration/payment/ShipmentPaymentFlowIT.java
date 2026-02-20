package com.shipmate.integration.payment;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.earning.DriverEarning;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.earning.DriverEarningRepository;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.earning.DriverEarningService;
import com.shipmate.service.shipment.ShipmentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ShipmentPaymentFlowIT extends AbstractIntegrationTest {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private DriverEarningService driverEarningService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private DriverEarningRepository driverEarningRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCapturePaymentAndCreateDriverEarning_whenShipmentDelivered() {

        User sender = createUser(UserType.SENDER);
        User driver = createUser(UserType.DRIVER);

        Shipment shipment = shipmentRepository.save(
                Shipment.builder()
                        .sender(sender)
                        .pickupAddress("Paris")
                        .pickupLatitude(BigDecimal.valueOf(48.8566))
                        .pickupLongitude(BigDecimal.valueOf(2.3522))
                        .deliveryAddress("Lyon")
                        .deliveryLatitude(BigDecimal.valueOf(45.7640))
                        .deliveryLongitude(BigDecimal.valueOf(4.8357))
                        .packageWeight(BigDecimal.valueOf(2))
                        .packageValue(BigDecimal.valueOf(100))
                        .requestedPickupDate(LocalDate.now())
                        .requestedDeliveryDate(LocalDate.now().plusDays(1))
                        .status(ShipmentStatus.ASSIGNED)
                        .basePrice(BigDecimal.valueOf(100))
                        .extraInsuranceFee(BigDecimal.ZERO)
                        .build()
        );

        Booking booking = Booking.builder()
                .driver(driver)
                .status(BookingStatus.IN_PROGRESS)
                .totalPrice(BigDecimal.valueOf(100))
                .platformCommission(BigDecimal.valueOf(10))
                .driverEarnings(BigDecimal.valueOf(90))
                .shipments(new ArrayList<>())
                .build();

        booking = bookingRepository.save(booking);

        shipment.setBooking(booking);
        booking.getShipments().add(shipment);

        shipmentRepository.save(shipment);


        Payment payment = paymentRepository.save(
                Payment.builder()
                        .shipment(shipment)
                        .sender(sender)
                        .stripePaymentIntentId("pi_test_" + UUID.randomUUID())
                        .amountTotal(BigDecimal.valueOf(100))
                        .currency("EUR")
                        .paymentStatus(PaymentStatus.AUTHORIZED)
                        .build()
        );

        shipmentService.markInTransit(shipment.getId(), driver.getId());

        assertThat(
                shipmentRepository.findById(shipment.getId()).orElseThrow().getStatus()
        ).isEqualTo(ShipmentStatus.IN_TRANSIT);

        shipmentService.markDelivered(shipment.getId(), driver.getId());

        Shipment delivered =
                shipmentRepository.findById(shipment.getId()).orElseThrow();

        assertThat(delivered.getStatus())
                .isEqualTo(ShipmentStatus.DELIVERED);

        payment.setPaymentStatus(PaymentStatus.CAPTURED);
        paymentRepository.save(payment);

        driverEarningService.createIfAbsent(payment);
        List<DriverEarning> earnings =
                driverEarningRepository.findAll();

        assertThat(earnings).hasSize(1);

        DriverEarning earning = earnings.get(0);

        assertThat(earning.getGrossAmount())
                .isEqualByComparingTo("100.00");

        assertThat(earning.getCommissionAmount())
                .isEqualByComparingTo("20.00");

        assertThat(earning.getNetAmount())
                .isEqualByComparingTo("80.00");
    }

    private User createUser(UserType type) {
        return userRepository.saveAndFlush(
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
}
