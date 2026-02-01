package com.shipmate.service.mail;


public interface MailService {
    void sendVerificationEmail(String toEmail, String verificationToken);
    void sendPasswordResetEmail(String toEmail, String resetToken);
    void sendDriverApprovedEmail(String toEmail);
    void sendDriverRejectedEmail(String toEmail);
    void sendDriverSuspendedEmail(String toEmail);
}
