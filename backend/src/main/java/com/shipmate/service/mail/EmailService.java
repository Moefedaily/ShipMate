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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

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

    @Value("${app.mail.support-email}")
    private String supportEmail;


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

    @Override
    public void sendDriverApprovedEmail(String toEmail) {
        Context context = new Context();
        context.setVariable("dashboardLink", baseUrl + "/dashboard/driver");

        String html = templateEngine.process(
            "email/driver-approved",
            context
        );

        sendHtmlEmail(
            toEmail,
            "You're approved to drive with ShipMate",
            html
        );

        log.info("Driver approved email sent to {}", toEmail);
    }

    @Override
    public void sendDriverRejectedEmail(String toEmail) {
        Context context = new Context();

        String html = templateEngine.process(
            "email/driver-rejected",
            context
        );

        sendHtmlEmail(
            toEmail,
            "Your driver application status",
            html
        );
    }

    @Override
    public void sendDriverSuspendedEmail(String toEmail) {
        Context context = new Context();

        String html = templateEngine.process(
            "email/driver-suspended",
            context
        );

        sendHtmlEmail(
            toEmail,
            "Your driver account has been suspended",
            html
        );
    }

    @Override
    public void sendPaymentRequiredEmail( String toEmail, UUID shipmentId, BigDecimal amount, String paymentLink ) {

        String formattedAmount = amount.setScale(2, RoundingMode.HALF_UP).toString();

        String currency = "EUR";

        Context context = new Context();
        context.setVariable("emailTitle", "Complete Your Payment");
        context.setVariable("shipmentId", shipmentId.toString());
        context.setVariable("amount", formattedAmount);
        context.setVariable("currency", currency);
        context.setVariable("paymentLink", paymentLink);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("currentYear", Year.now().getValue());

        String html = templateEngine.process(
                "email/payment-required",
                context
        );

        sendHtmlEmail(
                toEmail,
                "Complete your payment - ShipMate",
                html
        );

        log.info("Payment required email sent to {}", toEmail);
    }

        @Override
    public void sendPaymentReceiptEmail(String toEmail, UUID shipmentId, BigDecimal amount ) {

        String formattedAmount = amount.setScale(2, RoundingMode.HALF_UP).toString();
        String formattedDate = DateTimeFormatter.ofPattern("MMM dd, yyyy")
                .withLocale(Locale.ENGLISH)
                .format(Instant.now().atZone(ZoneId.systemDefault()).toLocalDate());

        String currency = "EUR";
        Context context = new Context();
        context.setVariable("shipmentId", shipmentId.toString());
        context.setVariable("formattedAmount", formattedAmount);
        context.setVariable("currency", currency);
        context.setVariable("receiptDate", formattedDate);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("currentYear", Year.now().getValue());


        String html = templateEngine.process(
                "email/payment-receipt",
                context
        );

        sendHtmlEmail(
                toEmail,
                "Payment receipt - ShipMate",
                html
        );

        log.info("Payment receipt email sent to {}", toEmail);
    }

    @Override
    public void sendPaymentRefundedEmail(String toEmail, UUID shipmentId, BigDecimal amount) {
        String formattedDate = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        .withLocale(Locale.ENGLISH)
        .format(Instant.now().atZone(ZoneId.systemDefault()).toLocalDate());

        String formattedAmount = amount.setScale(2, RoundingMode.HALF_UP).toString();

        Context context = new Context();
        context.setVariable("shipmentId", shipmentId);
        context.setVariable("amount", amount);
        context.setVariable("formattedAmount", formattedAmount);
        context.setVariable("refundDate", formattedDate);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("currentYear", Year.now().getValue());
        String html = templateEngine.process(
                "email/payment-refunded",
                context
        );

        sendHtmlEmail(
                toEmail,
                "Your payment has been refunded - ShipMate",
                html
        );

        log.info("Refund email sent to {}", toEmail);
    }


}
