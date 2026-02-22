package com.shipmate.integration.payment;

import com.shipmate.config.AbstractIntegrationTest;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.earning.EarningType;
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private DriverEarningRepository driverEarningRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void webhook_shouldCapturePayment_andCreateDriverEarning_idempotent() throws Exception {


        User sender = createUser(UserType.SENDER);
        User driver = createUser(UserType.DRIVER);

        Booking booking = bookingRepository.save(
                Booking.builder()
                        .driver(driver)
                        .status(BookingStatus.IN_PROGRESS)
                        .shipments(new ArrayList<>())
                        .build()
        );

        Shipment shipment = shipmentRepository.save(
                Shipment.builder()
                        .sender(sender)
                        .booking(booking)
                        .status(ShipmentStatus.ASSIGNED)
                        .pickupAddress("Paris")
                        .pickupLatitude(BigDecimal.ONE)
                        .pickupLongitude(BigDecimal.ONE)
                        .deliveryAddress("Lyon")
                        .deliveryLatitude(BigDecimal.TEN)
                        .deliveryLongitude(BigDecimal.TEN)
                        .packageWeight(BigDecimal.ONE)
                        .packageValue(BigDecimal.TEN)
                        .requestedPickupDate(LocalDate.now())
                        .requestedDeliveryDate(LocalDate.now().plusDays(1))
                        .basePrice(BigDecimal.valueOf(100))
                        .insuranceSelected(false)
                        .insuranceFee(BigDecimal.ZERO.setScale(2))
                        .declaredValue(null)
                        .insuranceCoverageAmount(null)
                        .insuranceDeductibleRate(null)
                        .deliveredAt(Instant.now())
                        .build()
        );

        booking.getShipments().add(shipment);
        bookingRepository.save(booking);

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

        String payload = """
        {
        "id": "evt_test_123",
        "object": "event",
        "api_version": "2024-06-20",
        "type": "payment_intent.succeeded",
        "data": {
            "object": {
            "id": "%s",
            "object": "payment_intent",
            "amount": 10000,
            "currency": "eur",
            "status": "succeeded"
            }
        }
        }
        """.formatted(payment.getStripePaymentIntentId());



        mockMvc.perform(post("/api/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", "test")
                .content(payload))
            .andExpect(status().isOk());


        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();

        assertThat(updated.getPaymentStatus())
                .isEqualTo(PaymentStatus.CAPTURED);

        assertThat(
                driverEarningRepository
                        .existsByPaymentAndEarningType(updated, EarningType.ORIGINAL)
        ).isTrue();

        assertThat(
                driverEarningRepository
                        .existsByPaymentAndEarningType(updated, EarningType.REFUND)
        ).isFalse();

        long earningCountAfterFirstCall = driverEarningRepository.count();


        mockMvc.perform(post("/api/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", "test")
                .content(payload))
            .andExpect(status().isOk());

        long earningCountAfterSecondCall = driverEarningRepository.count();

        assertThat(earningCountAfterSecondCall)
                .isEqualTo(earningCountAfterFirstCall);

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
