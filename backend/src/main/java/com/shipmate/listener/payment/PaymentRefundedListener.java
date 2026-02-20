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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRefundedListener {

    private final ShipmentRepository shipmentRepository;
    private final NotificationRepository notificationRepository;
    private final MailService mailService;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentRefunded(PaymentRefundedEvent event) {

        Shipment shipment = shipmentRepository
                .findById(event.shipmentId())
                .orElseThrow();

        var sender = shipment.getSender();

        boolean alreadyNotified =
        notificationRepository
                .existsByUser_IdAndReferenceIdAndReferenceTypeAndTitle(
                        sender.getId(),
                        shipment.getId(),
                        ReferenceType.SHIPMENT,
                        "Payment Refunded"
                );

        if (alreadyNotified) {
            log.info("[PAYMENT] Refund already processed shipmentId={}",
                    shipment.getId());
            return;
        }

        eventPublisher.publishEvent(
                new NotificationRequestedEvent(
                        sender.getId(),
                        "Payment Refunded",
                        "Your payment has been refunded successfully.",
                        NotificationType.PAYMENT_STATUS,
                        shipment.getId(),
                        ReferenceType.SHIPMENT
                )
        );

        mailService.sendPaymentRefundedEmail(
                sender.getEmail(),
                shipment.getId(),
                event.amount()
        );

        log.info("[PAYMENT] Refund email + notification sent shipmentId={}",
                shipment.getId());
    }

}
