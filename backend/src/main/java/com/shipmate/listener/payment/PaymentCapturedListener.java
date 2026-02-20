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
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCapturedListener {

    private final ShipmentRepository shipmentRepository;
    private final NotificationRepository notificationRepository;
    private final MailService mailService;
    private final ApplicationEventPublisher eventPublisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCaptured(PaymentCapturedEvent event) {

        Shipment shipment = shipmentRepository
                .findById(event.shipmentId())
                .orElseThrow();

        var sender = shipment.getSender();

        boolean alreadyProcessed =
                notificationRepository
                    .existsByUser_IdAndReferenceIdAndReferenceTypeAndTitle(
                            sender.getId(),
                            shipment.getId(),
                            ReferenceType.SHIPMENT,
                            "Payment Completed"
                    );

        if (alreadyProcessed) {
            log.info("[PAYMENT] Capture already processed shipmentId={}",
                    shipment.getId());
            return;
        }

        eventPublisher.publishEvent(
                new NotificationRequestedEvent(
                        sender.getId(),
                        "Payment Completed",
                        "Your payment has been successfully processed. Delivery confirmed.",
                        NotificationType.PAYMENT_STATUS,
                        shipment.getId(),
                        ReferenceType.SHIPMENT
                )
        );

        mailService.sendPaymentReceiptEmail(
                sender.getEmail(),
                shipment.getId(),
                event.amount()
        );

        log.info("[PAYMENT] Receipt email + notification sent shipmentId={}",
                shipment.getId());
    }

}
