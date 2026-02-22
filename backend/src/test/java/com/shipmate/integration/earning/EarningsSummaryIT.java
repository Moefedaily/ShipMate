package com.shipmate.integration.earning;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EarningsSummaryIT extends AbstractIntegrationTest {

    @Autowired
    private DriverEarningService driverEarningService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverEarningRepository driverEarningRepository;
    @Test
    void earningsSummary_shouldAggregateGrossCommissionNetCorrectly() {

        User driver = createUser(UserType.DRIVER);
        User sender = createUser(UserType.SENDER);

        Booking booking = bookingRepository.save(
                Booking.builder()
                        .driver(driver)
                        .status(BookingStatus.COMPLETED)
                        .shipments(new ArrayList<>())
                        .build()
        );

        // Create two captured payments
        Payment payment1 = createCapturedPayment(driver, sender, booking, BigDecimal.valueOf(100));
        Payment payment2 = createCapturedPayment(driver, sender, booking, BigDecimal.valueOf(200));
        System.out.println(payment2);

        // Refund the FIRST payment
        payment1.setPaymentStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment1);
        driverEarningService.createRefundAdjustment(payment1);

        var summary = driverEarningService.getMyEarningsSummary(driver.getId());

        assertThat(driverEarningRepository.count()).isEqualTo(3);

        assertThat(summary.getTotalGross()).isEqualByComparingTo("200.00");
        assertThat(summary.getTotalCommission()).isEqualByComparingTo("40.00");
        assertThat(summary.getTotalNet()).isEqualByComparingTo("160.00");
    }


        private Payment createCapturedPayment(User driver,
                                        User sender,
                                        Booking booking,
                                        BigDecimal amount) {

        Shipment shipment = shipmentRepository.save(
                Shipment.builder()
                        .sender(sender)
                        .booking(booking)
                        .status(ShipmentStatus.DELIVERED)
                        .pickupAddress("A")
                        .pickupLatitude(BigDecimal.ONE)
                        .pickupLongitude(BigDecimal.ONE)
                        .deliveryAddress("B")
                        .deliveryLatitude(BigDecimal.TEN)
                        .deliveryLongitude(BigDecimal.TEN)
                        .packageWeight(BigDecimal.ONE)
                        .packageValue(BigDecimal.TEN)
                        .requestedPickupDate(LocalDate.now())
                        .requestedDeliveryDate(LocalDate.now().plusDays(1))
                        .basePrice(amount)
                        .insuranceSelected(false)
                        .insuranceFee(BigDecimal.ZERO.setScale(2))
                        .declaredValue(null)
                        .insuranceCoverageAmount(null)
                        .insuranceDeductibleRate(null)
                        .deliveredAt(Instant.now())
                        .build()
        );

        booking.getShipments().add(shipment);

        Payment payment = paymentRepository.save(
                Payment.builder()
                        .shipment(shipment)
                        .sender(sender)
                        .stripePaymentIntentId("pi_test_" + UUID.randomUUID())
                        .amountTotal(amount)
                        .currency("EUR")
                        .paymentStatus(PaymentStatus.CAPTURED)
                        .build()
        );

        driverEarningService.createIfAbsent(payment);

        return payment;
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
    
}
