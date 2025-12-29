package com.shipmate.unit.service.shipment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shipmate.dto.request.shipment.CreateShipmentRequest;
import com.shipmate.dto.request.shipment.UpdateShipmentRequest;
import com.shipmate.dto.response.shipment.ShipmentResponse;
import com.shipmate.mapper.ShipmentMapper;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.model.user.User;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.shipment.ShipmentService;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShipmentMapper shipmentMapper;

    @InjectMocks
    private ShipmentService shipmentService;

    /* =========================
       CREATE
       ========================= */

    @Test
    void create_shouldCreateShipment_whenUserExists() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();

        CreateShipmentRequest request = new CreateShipmentRequest();
        request.setPickupAddress("Paris");
        request.setPickupLatitude(BigDecimal.valueOf(48.8566));
        request.setPickupLongitude(BigDecimal.valueOf(2.3522));
        request.setDeliveryAddress("Lyon");
        request.setDeliveryLatitude(BigDecimal.valueOf(45.7640));
        request.setDeliveryLongitude(BigDecimal.valueOf(4.8357));
        request.setPackageWeight(BigDecimal.valueOf(2.5));
        request.setPackageValue(BigDecimal.valueOf(100));
        request.setRequestedPickupDate(LocalDate.now());
        request.setRequestedDeliveryDate(LocalDate.now().plusDays(1));
        request.setBasePrice(BigDecimal.valueOf(20));

        Shipment shipment = Shipment.builder().build();
        Shipment savedShipment = Shipment.builder()
                .status(ShipmentStatus.CREATED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentMapper.toEntity(request)).thenReturn(shipment);
        when(shipmentRepository.save(shipment)).thenReturn(savedShipment);
        when(shipmentMapper.toResponse(savedShipment)).thenReturn(new ShipmentResponse());

        ShipmentResponse response = shipmentService.create(userId, request);

        assertThat(response).isNotNull();
        verify(userRepository).findById(userId);
        verify(shipmentMapper).toEntity(request);
        verify(shipmentRepository).save(shipment);
        verify(shipmentMapper).toResponse(savedShipment);
    }

    @Test
    void create_shouldFail_whenUserNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                shipmentService.create(userId, new CreateShipmentRequest())
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("User not found");

        verify(shipmentRepository, never()).save(any());
    }

    /* =========================
       UPDATE
       ========================= */

    @Test
    void update_shouldFail_whenShipmentNotCreated() {
        UUID userId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();

        User user = User.builder().id(userId).build();
        Shipment shipment = Shipment.builder()
                .id(shipmentId)
                .sender(user)
                .status(ShipmentStatus.ASSIGNED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentRepository.findByIdAndSender(shipmentId, user))
                .thenReturn(Optional.of(shipment));

        UpdateShipmentRequest request = new UpdateShipmentRequest();

        assertThatThrownBy(() ->
                shipmentService.update(shipmentId, userId, request)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("no longer be modified");

        verify(shipmentMapper, never()).updateEntity(any(), any());
    }

    /* =========================
       DELETE
       ========================= */

    @Test
    void delete_shouldDeleteShipment_whenCreated() {
        UUID userId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();

        User user = User.builder().id(userId).build();
        Shipment shipment = Shipment.builder()
                .id(shipmentId)
                .sender(user)
                .status(ShipmentStatus.CREATED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentRepository.findByIdAndSender(shipmentId, user))
                .thenReturn(Optional.of(shipment));

        shipmentService.delete(shipmentId, userId);

        verify(shipmentRepository).delete(shipment);
    }

    @Test
    void delete_shouldFail_whenNotCreated() {
        UUID userId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();

        User user = User.builder().id(userId).build();
        Shipment shipment = Shipment.builder()
                .id(shipmentId)
                .sender(user)
                .status(ShipmentStatus.DELIVERED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shipmentRepository.findByIdAndSender(shipmentId, user))
                .thenReturn(Optional.of(shipment));

        assertThatThrownBy(() ->
                shipmentService.delete(shipmentId, userId)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("no longer be deleted");

        verify(shipmentRepository, never()).delete(any());
    }
}
