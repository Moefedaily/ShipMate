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

public interface MessageRepository extends JpaRepository<Message, UUID> {

    long countByMessageType(MessageType type);

    Page<Message> findByShipment_IdOrderBySentAtAsc(
        UUID shipmentId,
        Pageable pageable
    );

    long countByShipment_IdAndReceiver_IdAndIsReadFalse(
        UUID shipmentId,
        UUID receiverId
    );

    Page<Message> findByShipment_IdAndMessageTypeOrderBySentAtAsc(
        UUID shipmentId,
        MessageType messageType,
        Pageable pageable
    );

    Page<Message> findByShipment_IdAndSender_IdAndReceiver_IdOrderBySentAtAsc(
        UUID shipmentId,
        UUID senderId,
        UUID receiverId,
        Pageable pageable
    );

    @Query("""
        SELECT m FROM Message m
        WHERE m.shipment.id = :shipmentId
        ORDER BY m.sentAt DESC
    """)
    Page<Message> findLatestByShipment(
        @Param("shipmentId") UUID shipmentId,
        Pageable pageable
    );

    List<Message> findByShipment_Id(UUID shipmentId);

    @Modifying(clearAutomatically = true)
    @Query("""
        update Message m
        set m.isRead = true
        where m.shipment.id = :shipmentId
        and m.receiver.id = :userId
        and m.isRead = false
    """)
    void markAllAsRead(UUID shipmentId, UUID userId);


    @Query("""
        select m from Message m
        join fetch m.shipment sh
        join fetch m.sender s
        join fetch m.receiver r
        where m.id = :id
    """)
    Message findWithRelationsById(@Param("id") UUID id);
}
