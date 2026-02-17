package com.shipmate.unit.service.earning;

import com.shipmate.model.earning.DriverEarning;
import com.shipmate.model.earning.EarningType;
import com.shipmate.model.earning.PayoutStatus;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.user.User;
import com.shipmate.repository.earning.DriverEarningRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.earning.DriverEarningService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverEarningServiceTest {

    @Mock
    private DriverEarningRepository driverEarningRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DriverEarningService driverEarningService;

    private User driver;
    private Shipment shipment;
    private Payment payment;

    @BeforeEach
    void setup() {

        ReflectionTestUtils.setField(driverEarningService,
                "commissionRate",
                new BigDecimal("0.10"));

        driver = User.builder()
                .id(UUID.randomUUID())
                .build();

        shipment = Shipment.builder()
                .id(UUID.randomUUID())
                .booking(Booking.builder().driver(driver).build())
                .build();

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .shipment(shipment)
                .amountTotal(new BigDecimal("100.00"))
                .paymentStatus(PaymentStatus.CAPTURED)
                .build();
    }

    @Test
    void createIfAbsent_shouldCreateOriginalEarning_whenNotExists() {

        when(driverEarningRepository
                .existsByPaymentAndEarningType(payment, EarningType.ORIGINAL))
                .thenReturn(false);

        driverEarningService.createIfAbsent(payment);

        verify(driverEarningRepository, times(1))
                .save(any(DriverEarning.class));
    }

    @Test
    void createIfAbsent_shouldNotCreateDuplicateOriginal() {

        when(driverEarningRepository
                .existsByPaymentAndEarningType(payment, EarningType.ORIGINAL))
                .thenReturn(true);

        driverEarningService.createIfAbsent(payment);

        verify(driverEarningRepository, never())
                .save(any());
    }

    @Test
    void createIfAbsent_shouldCalculateCommissionCorrectly() {

        when(driverEarningRepository
                .existsByPaymentAndEarningType(payment, EarningType.ORIGINAL))
                .thenReturn(false);

        driverEarningService.createIfAbsent(payment);

        verify(driverEarningRepository).save(argThat(earning ->

                earning.getEarningType() == EarningType.ORIGINAL &&
                earning.getGrossAmount().compareTo(new BigDecimal("100.00")) == 0 &&
                earning.getCommissionAmount().compareTo(new BigDecimal("10.00")) == 0 &&
                earning.getNetAmount().compareTo(new BigDecimal("90.00")) == 0 &&
                earning.getPayoutStatus() == PayoutStatus.PENDING
        ));
    }

    @Test
    void createIfAbsent_shouldFail_whenDriverMissing() {

        shipment.setBooking(Booking.builder().driver(null).build());

        assertThatThrownBy(() ->
                driverEarningService.createIfAbsent(payment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("assigned driver");
    }
}
