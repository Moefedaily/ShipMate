package com.shipmate.dev.fixtures;

import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final DriverProfileRepository driverProfileRepository;

    @Override
    public void run(ApplicationArguments args) {

        if (shipmentRepository.count() > 0) {
            return; // already seeded
        }

        // Senders
        List<User> senders = ensureSenders(4);

        // Drivers
        List<User> drivers = ensureDrivers(5);
        approveDrivers(drivers);

        // Shipments
        for (User sender : senders) {
            for (int i = 0; i < 5; i++) {

                var pickup = GrenobleLocations.random();
                var delivery = GrenobleLocations.random();

                if (pickup.equals(delivery)) continue;

                shipmentRepository.save(
                        ShipmentFixtureFactory.create(sender, pickup, delivery)
                );
            }
        }

        System.out.println(" Dev fixtures loaded");
    }

    private List<User> ensureSenders(int count) {
        List<User> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int index = i;
            String email = "sender" + index + "@dev.shipmate.io";
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> userRepository.save(
                            User.builder()
                                    .email(email)
                                    .firstName("Sender")
                                    .lastName("Dev" + index)
                                    .userType(UserType.SENDER)
                                    .active(true)
                                    .verified(true)
                                    .password("{noop}password")
                                    .build()
                    ));
            result.add(user);
        }
        return result;
    }

    private List<User> ensureDrivers(int count) {
        List<User> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int index = i;
            String email = "driver" + index + "@dev.shipmate.io";
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> userRepository.save(
                            User.builder()
                                    .email(email)
                                    .firstName("Driver")
                                    .lastName("Dev" + index)
                                    .userType(UserType.DRIVER)
                                    .active(true)
                                    .verified(true)
                                    .password("{noop}password")
                                    .build()
                    ));
            result.add(user);
        }
        return result;
    }
    private void approveDrivers(List<User> drivers) {

        for (int i = 0; i < drivers.size(); i++) {

            var driver = drivers.get(i);
            var location = GrenobleLocations.ALL.get(i % GrenobleLocations.ALL.size());

            if (driverProfileRepository.existsByUser(driver)) continue;

            driverProfileRepository.save(
                    DriverFixtureFactory.approvedDriver(driver, location, i)
            );
        }
    }
}
