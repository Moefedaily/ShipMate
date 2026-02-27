package com.shipmate.mapper.shipment;

import org.springframework.stereotype.Component;

import com.shipmate.dto.response.driver.AssignedDriverResponse;
import com.shipmate.dto.response.shipment.ShipmentResponse;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;
import com.shipmate.repository.driver.DriverProfileRepository;
import com.shipmate.repository.payment.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShipmentAssembler {

    private final ShipmentMapper shipmentMapper;
    private final DriverProfileRepository driverProfileRepository;
    private final PaymentRepository paymentRepository;


    public ShipmentResponse toResponse(Shipment shipment) {

        ShipmentResponse response = shipmentMapper.toResponse(shipment);

        if (shipment.getBooking() != null &&
            shipment.getBooking().getDriver() != null) {

            User driver = shipment.getBooking().getDriver();

            DriverProfile profile = driverProfileRepository
                    .findByUser_Id(driver.getId())
                    .orElse(null);

            if (profile != null) {
                response.setDriver(
                    AssignedDriverResponse.builder()
                        .id(driver.getId())
                        .firstName(driver.getFirstName())
                        .avatarUrl(driver.getAvatarUrl())
                        .vehicleType(profile.getVehicleType())
                        .build()
                );
            }
        }

            paymentRepository.findByShipment(shipment)
            .map(Payment::getPaymentStatus)
            .ifPresent(response::setPaymentStatus);
            
        return response;
    }
}
