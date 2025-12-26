package com.shipmate.service.mail;


public interface MailService {
    void sendVerificationEmail(String toEmail, String verificationToken);
    void sendPasswordResetEmail(String toEmail, String resetToken);
}
