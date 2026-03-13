package com.shipmate.repository.booking;

import com.shipmate.model.booking.Booking;
import com.shipmate.model.booking.BookingStatus;
import com.shipmate.model.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @EntityGraph(attributePaths = {"driver.avatar", "shipments", "shipments.photos"})
    List<Booking> findByDriver(User driver);

    @Query("""
    SELECT DISTINCT b FROM Booking b
    LEFT JOIN FETCH b.driver d
    LEFT JOIN FETCH d.avatar
    LEFT JOIN FETCH b.shipments s
    LEFT JOIN FETCH s.sender
    LEFT JOIN FETCH s.photos
    WHERE b.id = :id
    """)
    Optional<Booking> findWithShipmentsById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {
        "driver.avatar",
        "shipments",
        "shipments.sender",
        "shipments.photos"
    })
    Optional<Booking> findFirstByDriverAndStatusInOrderByCreatedAtDesc(
        User driver,
        List<BookingStatus> statuses
    );
    @Query("""
    SELECT DISTINCT b FROM Booking b
    LEFT JOIN b.shipments s
    WHERE
        b.driver.id = :userId
        OR s.sender.id = :userId
    """)
    List<Booking> findAllUserBookings(@Param("userId") UUID userId);


}
