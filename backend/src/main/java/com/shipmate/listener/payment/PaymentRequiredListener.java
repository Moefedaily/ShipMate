package com.shipmate.listener.payment;

import com.shipmate.listener.notification.NotificationRequestedEvent;
import com.shipmate.model.notification.NotificationType;
import com.shipmate.model.notification.ReferenceType;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.repository.notification.NotificationRepository;
import com.shipmate.repository.shipment.ShipmentRepository;
import com.shipmate.service.mail.MailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRequiredListener {

    private final ShipmentRepository shipmentRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MailService mailService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void onPaymentRequired(PaymentRequiredEvent event) {

        Shipment shipment = shipmentRepository
                .findById(event.shipmentId())
                .orElseThrow();

        var sender = shipment.getSender();
        UUID shipmentId = shipment.getId();

        boolean alreadyNotified =
                notificationRepository
                        .existsByUser_IdAndReferenceIdAndReferenceTypeAndTitle(
                                sender.getId(),
                                shipmentId,
                                ReferenceType.SHIPMENT,
                                "Payment Required"
                        );
        if (alreadyNotified) {
                log.info("[PAYMENT] PaymentRequired already processed shipmentId={}",
                        shipmentId);
                return;
        }

        BigDecimal totalAmount =
                shipment.getBasePrice().add(
                        shipment.getExtraInsuranceFee() != null
                                ? shipment.getExtraInsuranceFee()
                                : BigDecimal.ZERO
                );

        String paymentLink =
                frontendBaseUrl +
                "/dashboard/shipments/" +
                shipmentId +
                "/payment";

        eventPublisher.publishEvent(
                new NotificationRequestedEvent(
                        sender.getId(),
                        "Payment Required",
                        "Please complete your payment to allow delivery to begin.",
                        NotificationType.PAYMENT_STATUS,
                        shipmentId,
                        ReferenceType.SHIPMENT
                )
        );

        mailService.sendPaymentRequiredEmail(
                sender.getEmail(),
                shipmentId,
                totalAmount,
                paymentLink
        );

        log.info("[PAYMENT] PaymentRequired notification + email sent shipmentId={}",
                shipmentId);
        }

}
