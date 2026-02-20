package com.shipmate.service.mail;

import java.math.BigDecimal;
import java.util.UUID;

public interface MailService {
    void sendVerificationEmail(String toEmail, String verificationToken);
    void sendPasswordResetEmail(String toEmail, String resetToken);
    void sendDriverApprovedEmail(String toEmail);
    void sendDriverRejectedEmail(String toEmail);
    void sendDriverSuspendedEmail(String toEmail);
    void sendPaymentReceiptEmail( String toEmail, UUID shipmentId, BigDecimal amount );
    void sendPaymentRefundedEmail( String toEmail, UUID shipmentId, BigDecimal amount);
    void sendPaymentRequiredEmail( String toEmail, UUID shipmentId, BigDecimal amount, String paymentLink );

}
