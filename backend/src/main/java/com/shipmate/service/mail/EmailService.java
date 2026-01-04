package com.shipmate.service.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Profile("!test")
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.mail.base-url}")
    private String baseUrl;

    @Override
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        Context context = new Context();
        context.setVariable("verificationLink", baseUrl + "/verify-email?token=" + verificationToken);

        String htmlContent = templateEngine.process("email/verification", context);
        sendHtmlEmail(toEmail, "Verify your SHIPMATE account", htmlContent);
        log.info("Verification email sent to: {}", toEmail);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        Context context = new Context();
        context.setVariable("resetLink", baseUrl + "/reset-password?token=" + resetToken);

        String htmlContent = templateEngine.process("email/password-reset", context);
        sendHtmlEmail(toEmail, "Reset your SHIPMATE password", htmlContent);
        log.info("Password reset email sent to: {}", toEmail);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (UnsupportedEncodingException | jakarta.mail.MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
