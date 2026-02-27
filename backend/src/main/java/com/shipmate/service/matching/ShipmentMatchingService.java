package com.shipmate.service.matching;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shipmate.dto.response.matching.MatchResultResponse;
import com.shipmate.dto.response.matching.MatchingMetricsResponse;
import com.shipmate.mapper.matching.MatchResultMapper;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.matching.MatchingMetrics;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.repository.booking.BookingRepository;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.util.DistanceCalculator;

@Service
@Transactional(readOnly = true)
public class ShipmentMatchingService {


    private static final double MAX_PICKUP_DETOUR_KM = 8.0;
    private static final double MAX_ALLOWED_DETOUR_KM = 15.0;


    private final ShipmentRepository shipmentRepository;
    private final BookingRepository bookingRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final MatchResultMapper matchResultMapper;

    public ShipmentMatchingService(
            ShipmentRepository shipmentRepository,
            BookingRepository bookingRepository,
            DriverProfileRepository driverProfileRepository,
            MatchResultMapper matchResultMapper
    ) {
        this.shipmentRepository = shipmentRepository;
        this.bookingRepository = bookingRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.matchResultMapper = matchResultMapper;
    }


    public List<MatchResultResponse> matchShipments(
            UUID driverId,
            Double driverLat,
            Double driverLng,
            UUID bookingId,
            double radiusKm,
            int maxResults,
            Pageable pageable
    ) {

        DriverProfile driverProfile = driverProfileRepository
                .findByUser_Id(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver profile not found"));


        double effectiveLat;
        double effectiveLng;

        if (driverLat != null && driverLng != null) {
            effectiveLat = driverLat;
            effectiveLng = driverLng;
        } else if (driverProfile.getLastLatitude() != null
                && driverProfile.getLastLongitude() != null) {

            effectiveLat = driverProfile.getLastLatitude().doubleValue();
            effectiveLng = driverProfile.getLastLongitude().doubleValue();
        } else {
            throw new IllegalArgumentException("Driver location is required");
        }

        Booking booking = resolveBookingIfPresent(bookingId, driverId);

        BigDecimal remainingCapacity = calculateRemainingCapacity(driverProfile, booking);

        Page<Shipment> page = shipmentRepository.findByStatus(
                ShipmentStatus.CREATED,
                pageable
        );

        List<MatchResultResponse> results = new ArrayList<>();

        for (Shipment shipment : page.getContent()) {

            if (shipment.getBooking() != null) {
                continue;
            }

            if (!isWeightCompatible(shipment, remainingCapacity)) {
                continue;
            }

            double distanceToPickup = DistanceCalculator.kilometers(
                    effectiveLat,
                    effectiveLng,
                    shipment.getPickupLatitude().doubleValue(),
                    shipment.getPickupLongitude().doubleValue()
            );

            if (distanceToPickup > radiusKm) {
                continue;
            }

            Double detourKm = null;

            if (booking != null) {

                Shipment anchor = booking.getShipments().isEmpty()
                        ? null
                        : booking.getShipments().get(0);

                if (anchor != null) {

                    double pickupDetour = DistanceCalculator.kilometers(
                            anchor.getPickupLatitude().doubleValue(),
                            anchor.getPickupLongitude().doubleValue(),
                            shipment.getPickupLatitude().doubleValue(),
                            shipment.getPickupLongitude().doubleValue()
                    );

                    if (pickupDetour > MAX_PICKUP_DETOUR_KM) {
                        continue;
                    }

                    detourKm = pickupDetour;

                    if (detourKm > MAX_ALLOWED_DETOUR_KM) {
                        continue;
                    }
                }
            }


            double pickupToDelivery = DistanceCalculator.kilometers(
                    shipment.getPickupLatitude().doubleValue(),
                    shipment.getPickupLongitude().doubleValue(),
                    shipment.getDeliveryLatitude().doubleValue(),
                    shipment.getDeliveryLongitude().doubleValue()
            );

            int score = computeScore(
                    distanceToPickup,
                    shipment.getPackageWeight().doubleValue(),
                    remainingCapacity.doubleValue(),
                    detourKm
            );

            MatchingMetrics domainMetrics = new MatchingMetrics(
                    distanceToPickup,
                    pickupToDelivery,
                    detourKm,
                    score
            );

            MatchingMetricsResponse metricsResponse =
                    MatchingMetricsResponse.builder()
                            .distanceToPickupKm(domainMetrics.distanceToPickupKm())
                            .pickupToDeliveryKm(domainMetrics.pickupToDeliveryKm())
                            .estimatedDetourKm(domainMetrics.estimatedDetourKm())
                            .score(domainMetrics.score())
                            .build();

            results.add(matchResultMapper.toResponse(shipment, metricsResponse));
        }

        return results.stream()
                .sorted(
                        Comparator
                                .comparingInt(
                                        (MatchResultResponse r) ->
                                                r.getMetrics().getScore()
                                ).reversed()
                                .thenComparing(
                                        r -> r.getMetrics().getDistanceToPickupKm()
                                )
                )
                .limit(maxResults)
                .toList();
    }


    private Booking resolveBookingIfPresent(UUID bookingId, UUID driverId) {
        if (bookingId == null) {
            return null;
        }

        Booking booking = bookingRepository
                .findWithShipmentsById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getDriver().getId().equals(driverId)) {
            throw new IllegalArgumentException("Driver does not own this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking is not eligible for matching");
        }

        return booking;
    }

    private BigDecimal calculateRemainingCapacity(DriverProfile driver, Booking booking) {
        if (booking == null) {
            return driver.getMaxWeightCapacity();
        }

        BigDecimal used = booking.getShipments().stream()
                .map(Shipment::getPackageWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return driver.getMaxWeightCapacity()
                .subtract(used)
                .max(BigDecimal.ZERO);
    }

    private boolean isWeightCompatible(Shipment shipment, BigDecimal remainingCapacity) {
        return shipment.getPackageWeight().compareTo(remainingCapacity) <= 0;
    }

    private int computeScore(
            double distanceToPickup,
            double shipmentWeight,
            double remainingCapacity,
            Double detourKm
    ) {
        int score = 0;

        score += Math.max(0, 40 - (int) distanceToPickup);

        if (remainingCapacity > 0) {
            double ratio = shipmentWeight / remainingCapacity;
            score += (int) (30 * (1 - Math.min(ratio, 1)));
        }

        if (detourKm != null) {
            score += Math.max(0, 20 - detourKm.intValue());
        } else {
            score += 10;
        }

        return Math.min(score, 100);
    }
}
