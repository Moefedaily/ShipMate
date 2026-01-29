package com.shipmate.dev.fixtures;

import com.github.javafaker.Faker;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ShipmentFixtureFactory {

    private static final Faker faker = new Faker();

    public static Shipment create(
            User sender,
            GrenobleLocations.GeoPoint pickup,
            GrenobleLocations.GeoPoint delivery
    ) {

        return Shipment.builder()
                .sender(sender)

                .pickupAddress(pickup.label())
                .pickupLatitude(BigDecimal.valueOf(pickup.lat()))
                .pickupLongitude(BigDecimal.valueOf(pickup.lng()))

                .deliveryAddress(delivery.label())
                .deliveryLatitude(BigDecimal.valueOf(delivery.lat()))
                .deliveryLongitude(BigDecimal.valueOf(delivery.lng()))

                .packageDescription(faker.commerce().productName())
                .packageWeight(BigDecimal.valueOf(faker.number().randomDouble(2, 1, 25)))
                .packageValue(BigDecimal.valueOf(faker.number().randomDouble(2, 50, 800)))

                .requestedPickupDate(LocalDate.now().plusDays(faker.number().numberBetween(1, 3)))
                .requestedDeliveryDate(LocalDate.now().plusDays(faker.number().numberBetween(3, 6)))

                .basePrice(BigDecimal.valueOf(faker.number().randomDouble(2, 15, 120)))

                .status(ShipmentStatus.CREATED)
                .extraInsuranceFee(BigDecimal.ZERO)
                .photos(List.of())

                .build();
    }

    private ShipmentFixtureFactory() {}
}
