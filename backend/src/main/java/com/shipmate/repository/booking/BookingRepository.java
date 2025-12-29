package com.shipmate.repository.booking;

import com.shipmate.model.booking.Booking;
import com.shipmate.model.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @EntityGraph(attributePaths = "shipments")
    List<Booking> findByDriver(User driver);
}
