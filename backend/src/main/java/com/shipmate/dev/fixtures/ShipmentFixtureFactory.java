package com.shipmate.dev.fixtures;

import com.github.javafaker.Faker;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public final class ShipmentFixtureFactory {

    private static final Faker faker = new Faker();

    public static Shipment create(
            User sender,
            GrenobleLocations.GeoPoint pickup,
            GrenobleLocations.GeoPoint delivery
    ) {

        BigDecimal packageWeight = BigDecimal
                .valueOf(faker.number().randomDouble(2, 1, 25))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal packageValue = BigDecimal
                .valueOf(faker.number().randomDouble(2, 50, 800))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal basePrice = BigDecimal
                .valueOf(faker.number().randomDouble(2, 15, 120))
                .setScale(2, RoundingMode.HALF_UP);

        return Shipment.builder()
                .sender(sender)

                .pickupAddress(pickup.label())
                .pickupLatitude(BigDecimal.valueOf(pickup.lat()))
                .pickupLongitude(BigDecimal.valueOf(pickup.lng()))

                .deliveryAddress(delivery.label())
                .deliveryLatitude(BigDecimal.valueOf(delivery.lat()))
                .deliveryLongitude(BigDecimal.valueOf(delivery.lng()))

                .packageDescription(faker.commerce().productName())
                .packageWeight(packageWeight)
                .packageValue(packageValue)

                .requestedPickupDate(LocalDate.now().plusDays(faker.number().numberBetween(1, 3)))
                .requestedDeliveryDate(LocalDate.now().plusDays(faker.number().numberBetween(3, 6)))

                .basePrice(basePrice)

                // Insurance defaults (explicit and consistent)
                .insuranceSelected(false)
                .insuranceFee(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .declaredValue(null)
                .insuranceCoverageAmount(null)
                .insuranceDeductibleRate(null)

                .status(ShipmentStatus.CREATED)

                .photos(List.of(
                        "https://picsum.photos/id/20/800/600",
                        "https://picsum.photos/id/26/800/600",
                        "https://picsum.photos/id/250/800/600",
                        "https://picsum.photos/id/21/800/600",
                        "https://picsum.photos/id/27/800/600",
                        "https://picsum.photos/id/251/800/600",
                        "https://picsum.photos/id/22/800/600",
                        "https://picsum.photos/id/28/800/600",
                        "https://picsum.photos/id/252/800/600",
                        "https://picsum.photos/id/23/800/600",
                        "https://picsum.photos/id/29/800/600",
                        "https://picsum.photos/id/253/800/600",
                        "https://picsum.photos/id/24/800/600",
                        "https://picsum.photos/id/30/800/600",
                        "https://picsum.photos/id/254/800/600",
                        "https://picsum.photos/id/25/800/600",
                        "https://picsum.photos/id/31/800/600",
                        "https://picsum.photos/id/255/800/600"
                ))

                .deliveryLocked(false)

                .build();
    }

    private ShipmentFixtureFactory() {}
}