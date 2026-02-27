package com.shipmate.unit.service.earning;

import com.shipmate.model.earning.DriverEarning;
import com.shipmate.model.earning.EarningType;
import com.shipmate.model.earning.PayoutStatus;
import com.shipmate.model.payment.Payment;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverEarningRefundTest {

    @Mock
    private DriverEarningRepository driverEarningRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DriverEarningService driverEarningService;

    private User driver;
    private Shipment shipment;
    private Payment payment;
    private DriverEarning original;

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
                .build();

        original = DriverEarning.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .shipment(shipment)
                .payment(payment)
                .earningType(EarningType.ORIGINAL)
                .grossAmount(new BigDecimal("100.00"))
                .commissionAmount(new BigDecimal("10.00"))
                .netAmount(new BigDecimal("90.00"))
                .payoutStatus(PayoutStatus.PENDING)
                .build();
    }

    // ============================================================
    // CREATE REFUND ENTRY
    // ============================================================

    @Test
    void createRefundAdjustment_shouldCreateRefundEntry() {

        when(driverEarningRepository
                .findByPaymentAndEarningType(payment, EarningType.ORIGINAL))
                .thenReturn(Optional.of(original));

        when(driverEarningRepository
                .existsByPaymentAndEarningType(payment, EarningType.REFUND))
                .thenReturn(false);

        driverEarningService.createRefundAdjustment(payment);

        verify(driverEarningRepository).save(argThat(adjustment ->
                adjustment.getEarningType() == EarningType.REFUND &&
                adjustment.getGrossAmount().compareTo(new BigDecimal("-100.00")) == 0 &&
                adjustment.getCommissionAmount().compareTo(new BigDecimal("-10.00")) == 0 &&
                adjustment.getNetAmount().compareTo(new BigDecimal("-90.00")) == 0 &&
                adjustment.getPayoutStatus() == PayoutStatus.PENDING
        ));
    }

    // ============================================================
    // DO NOT DUPLICATE REFUND ENTRY
    // ============================================================

    @Test
    void createRefundAdjustment_shouldNotDuplicateRefundEntry() {

        when(driverEarningRepository
                .findByPaymentAndEarningType(payment, EarningType.ORIGINAL))
                .thenReturn(Optional.of(original));

        when(driverEarningRepository
                .existsByPaymentAndEarningType(payment, EarningType.REFUND))
                .thenReturn(true);

        driverEarningService.createRefundAdjustment(payment);

        verify(driverEarningRepository, never()).save(any());
    }

    // ============================================================
    // NO ORIGINAL → DO NOTHING
    // ============================================================

    @Test
    void createRefundAdjustment_shouldDoNothing_whenNoOriginal() {

        when(driverEarningRepository
                .findByPaymentAndEarningType(payment, EarningType.ORIGINAL))
                .thenReturn(Optional.empty());

        driverEarningService.createRefundAdjustment(payment);

        verify(driverEarningRepository, never()).save(any());
    }
}
