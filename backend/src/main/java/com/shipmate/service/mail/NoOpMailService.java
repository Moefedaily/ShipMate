package com.shipmate.service.mail;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("test")
@Service
@Slf4j
public class NoOpMailService implements MailService {
    @Override
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        log.info("[TEST] Verification email skipped. to={}, token={}", toEmail, verificationToken);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        log.info("[TEST] Password reset email skipped. to={}, token={}", toEmail, resetToken);
    }

    @Override
    public void sendDriverApprovedEmail(String toEmail) {
        log.info("[TEST] Driver approved email skipped. to={}", toEmail);
    }

    @Override
    public void sendDriverRejectedEmail(String toEmail) {
        log.info("[TEST] Driver rejected email skipped. to={}", toEmail);
    }

    @Override
    public void sendDriverSuspendedEmail(String toEmail) {
        log.info("[TEST] Driver suspended email skipped. to={}", toEmail);
    }
}
