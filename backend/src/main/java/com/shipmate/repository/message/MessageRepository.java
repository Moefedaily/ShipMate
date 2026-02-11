package com.shipmate.repository.message;

import com.shipmate.model.message.Message;
import com.shipmate.model.message.MessageType;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable; 
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
    long countByMessageType(MessageType type);
     Page<Message> findByBooking_IdOrderBySentAtAsc( UUID bookingId, Pageable pageable );

    long countByBooking_IdAndReceiver_IdAndIsReadFalse( UUID bookingId, UUID receiverId );

    Page<Message> findByBooking_IdAndMessageTypeOrderBySentAtAsc( UUID bookingId, MessageType messageType, Pageable pageable );

    Page<Message> findByBooking_IdAndSender_IdAndReceiver_IdOrderBySentAtAsc(UUID bookingId, UUID senderId, UUID receiverId, Pageable pageable );

    @Query("""
        SELECT m FROM Message m
        WHERE m.booking.id = :bookingId
        ORDER BY m.sentAt DESC
        """)
    Page<Message> findLatestByBooking( @Param("bookingId") UUID bookingId, Pageable pageable );

    List<Message> findByBooking_Id(UUID bookingId);

        @Modifying(clearAutomatically = true)
        @Query("""
        update Message m
        set m.isRead = true
        where m.booking.id = :bookingId
        and m.receiver.id = :userId
        and m.isRead = false
        """)
        void markAllAsRead(UUID bookingId, UUID userId);

        @Query("""
        select m from Message m
        join fetch m.booking b
        join fetch m.sender s
        join fetch m.receiver r
        where m.id = :id
    """)
    Message findWithRelationsById(@Param("id") Long id);


}
