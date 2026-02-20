package com.shipmate.unit.service.earning;

import com.shipmate.dto.response.earning.DriverEarningsSummaryResponse;
import com.shipmate.model.earning.PayoutStatus;
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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverEarningsSummaryTest {

    @Mock
    private DriverEarningRepository driverEarningRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DriverEarningService driverEarningService;

    private UUID driverId;
    private User driver;

    @BeforeEach
    void setup() {
        driverId = UUID.randomUUID();
        driver = User.builder().id(driverId).build();
    }

    @Test
    void getMyEarningsSummary_shouldReturnCorrectTotals() {

        when(userRepository.findById(driverId))
                .thenReturn(Optional.of(driver));

        when(driverEarningRepository.sumGrossByDriver(driver))
                .thenReturn(new BigDecimal("1000.00"));

        when(driverEarningRepository.sumCommissionByDriver(driver))
                .thenReturn(new BigDecimal("200.00"));

        when(driverEarningRepository.sumNetByDriver(driver))
                .thenReturn(new BigDecimal("800.00"));

        when(driverEarningRepository.sumNetByDriverAndStatus(driver, PayoutStatus.PENDING))
                .thenReturn(new BigDecimal("300.00"));

        when(driverEarningRepository.sumNetByDriverAndStatus(driver, PayoutStatus.PAID))
                .thenReturn(new BigDecimal("500.00"));

        DriverEarningsSummaryResponse summary =
                driverEarningService.getMyEarningsSummary(driverId);

        assertThat(summary.getTotalGross())
                .isEqualByComparingTo("1000.00");

        assertThat(summary.getTotalCommission())
                .isEqualByComparingTo("200.00");

        assertThat(summary.getTotalNet())
                .isEqualByComparingTo("800.00");

        assertThat(summary.getTotalPending())
                .isEqualByComparingTo("300.00");

        assertThat(summary.getTotalPaid())
                .isEqualByComparingTo("500.00");
    }


    @Test
    void getMyEarningsSummary_shouldHandleNullSums() {

        when(userRepository.findById(driverId))
                .thenReturn(Optional.of(driver));

        when(driverEarningRepository.sumGrossByDriver(driver))
                .thenReturn(null);

        when(driverEarningRepository.sumCommissionByDriver(driver))
                .thenReturn(null);

        when(driverEarningRepository.sumNetByDriver(driver))
                .thenReturn(null);

        when(driverEarningRepository.sumNetByDriverAndStatus(driver, PayoutStatus.PENDING))
                .thenReturn(null);

        when(driverEarningRepository.sumNetByDriverAndStatus(driver, PayoutStatus.PAID))
                .thenReturn(null);

        DriverEarningsSummaryResponse summary =
                driverEarningService.getMyEarningsSummary(driverId);

        assertThat(summary.getTotalGross()).isNull();
        assertThat(summary.getTotalCommission()).isNull();
        assertThat(summary.getTotalNet()).isNull();
        assertThat(summary.getTotalPending()).isNull();
        assertThat(summary.getTotalPaid()).isNull();
    }

    @Test
    void getMyEarningsSummary_shouldFail_whenDriverNotFound() {

        when(userRepository.findById(driverId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                driverEarningService.getMyEarningsSummary(driverId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Driver not found");
    }
}
