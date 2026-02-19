package com.shipmate.service.delivery;

import com.shipmate.dto.response.delivery.DeliveryCodeStatusResponse;
import com.shipmate.model.payment.Payment;
import com.shipmate.model.payment.PaymentStatus;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.shipment.ShipmentStatus;
import com.shipmate.repository.payment.PaymentRepository;
import com.shipmate.repository.shipment.ShipmentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryCodeService {

    @Value("${app.delivery.secret}")
    private String hmacSecret;

    @Value("${app.delivery.aeskey}")
    private String aesKeyBase64;

    @Value("${app.delivery.code-ttl-minutes}")
    private long ttlMinutes;

    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryCodeAttemptService attemptService;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int MAX_ATTEMPTS = 5;

    @Transactional
    public String generateAndStore(Shipment shipment) {

        if (shipment.getStatus() != ShipmentStatus.ASSIGNED) {
            throw new IllegalStateException(
                    "Delivery code can only be generated when shipment is ASSIGNED"
            );
        }

        if (isVerified(shipment)) {
            return null;
        }

        if (isExpired(shipment)) {
            clearCodeFields(shipment);
        }

        if (hasActiveCode(shipment)) {
            return null;
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        String salt = UUID.randomUUID().toString();

        Instant now = Instant.now();

        shipment.setDeliveryCodeSalt(salt);
        shipment.setDeliveryCodeHash(hash(code, salt));
        shipment.setDeliveryCodeCreatedAt(now);
        shipment.setDeliveryCodeAttempts(0);
        shipment.setDeliveryCodeVerifiedAt(null);

        EncryptionResult encrypted = encrypt(code);

        shipment.setDeliveryCodeEnc(encrypted.cipherText());
        shipment.setDeliveryCodeIv(encrypted.iv());

        shipmentRepository.save(shipment);

        return code;
    }

    @Transactional
    public void verify(Shipment shipment, String plainCode) {

        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            throw new IllegalStateException(
                    "Shipment not in correct state for delivery confirmation"
            );
        }

        if (!hasActiveCode(shipment)) {
            throw new IllegalStateException("No active delivery code");
        }

        if (isExpired(shipment)) {
            throw new IllegalStateException("Delivery code expired");
        }

        if (isVerified(shipment)) {
            throw new IllegalStateException("Delivery already confirmed");
        }

        Integer attempts = shipment.getDeliveryCodeAttempts();
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            throw new IllegalStateException("Too many invalid attempts");
        }

        String providedHash =
                hash(plainCode, shipment.getDeliveryCodeSalt());

        boolean matches = java.security.MessageDigest.isEqual(
                shipment.getDeliveryCodeHash().getBytes(StandardCharsets.UTF_8),
                providedHash.getBytes(StandardCharsets.UTF_8)
        );

        if (!matches) {
            attemptService.incrementAttempts(shipment.getId());
            throw new IllegalArgumentException("Invalid delivery code");
        }


        shipment.setDeliveryCodeVerifiedAt(Instant.now());
        shipment.setDeliveryCodeAttempts(0);

        shipmentRepository.save(shipment);
    }

    public DeliveryCodeStatusResponse getActiveCode(
            UUID shipmentId,
            UUID senderId
    ) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        if (!shipment.getSender().getId().equals(senderId)) {
            throw new AccessDeniedException("Not authorized");
        }

        if (!hasActiveCode(shipment)) {
            return null;
        }

        if (isExpired(shipment)) {
            return null;
        }

        if (isVerified(shipment)) {
            return null;
        }

        String plainCode = decrypt(
                shipment.getDeliveryCodeEnc(),
                shipment.getDeliveryCodeIv()
        );

        Instant expiresAt =
                shipment.getDeliveryCodeCreatedAt()
                        .plus(Duration.ofMinutes(ttlMinutes));

        return new DeliveryCodeStatusResponse(
                shipmentId,
                plainCode,
                expiresAt
        );
    }

    @Transactional
    public String reset(UUID shipmentId, UUID senderId) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));

        if (!shipment.getSender().getId().equals(senderId)) {
            throw new AccessDeniedException("Not authorized");
        }

        if (shipment.getStatus() != ShipmentStatus.ASSIGNED &&
            shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            throw new IllegalStateException("Cannot reset code in current state");
        }

        Payment payment = paymentRepository.findByShipment(shipment)
                .orElseThrow(() -> new IllegalStateException("Payment not found"));

        if (payment.getPaymentStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment must be AUTHORIZED");
        }

        if (isVerified(shipment)) {
            throw new IllegalStateException("Shipment already delivered");
        }

        clearCodeFields(shipment);
        shipmentRepository.save(shipment);

        return generateAndStore(shipment);
    }

    public boolean isVerified(Shipment shipment) {
        return shipment.getDeliveryCodeVerifiedAt() != null;
    }

    public boolean isExpired(Shipment shipment) {

        Instant created = shipment.getDeliveryCodeCreatedAt();
        if (created == null) return true;

        return Instant.now()
                .isAfter(created.plus(Duration.ofMinutes(ttlMinutes)));
    }

    private boolean hasActiveCode(Shipment shipment) {

        return shipment.getDeliveryCodeHash() != null &&
               shipment.getDeliveryCodeSalt() != null &&
               shipment.getDeliveryCodeCreatedAt() != null &&
               shipment.getDeliveryCodeEnc() != null &&
               shipment.getDeliveryCodeIv() != null;
    }

    private void clearCodeFields(Shipment shipment) {

        shipment.setDeliveryCodeHash(null);
        shipment.setDeliveryCodeSalt(null);
        shipment.setDeliveryCodeEnc(null);
        shipment.setDeliveryCodeIv(null);
        shipment.setDeliveryCodeCreatedAt(null);
        shipment.setDeliveryCodeAttempts(0);
        shipment.setDeliveryCodeVerifiedAt(null);
    }

    private String hash(String code, String salt) {

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));

            byte[] raw =
                    mac.doFinal((salt + code).getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(raw);

        } catch (Exception e) {
            throw new IllegalStateException("Hashing failed", e);
        }
    }

    private EncryptionResult encrypt(String plain) {

        try {
            byte[] keyBytes =
                    Base64.getDecoder().decode(aesKeyBase64);

            SecretKeySpec keySpec =
                    new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);

            Cipher cipher =
                    Cipher.getInstance("AES/GCM/NoPadding");

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    keySpec,
                    new GCMParameterSpec(128, iv)
            );

            byte[] encrypted =
                    cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            return new EncryptionResult(
                    Base64.getEncoder().encodeToString(encrypted),
                    Base64.getEncoder().encodeToString(iv)
            );

        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    private String decrypt(String cipherTextBase64, String ivBase64) {

        try {
            byte[] keyBytes =
                    Base64.getDecoder().decode(aesKeyBase64);

            SecretKeySpec keySpec =
                    new SecretKeySpec(keyBytes, "AES");

            byte[] iv =
                    Base64.getDecoder().decode(ivBase64);

            byte[] cipherBytes =
                    Base64.getDecoder().decode(cipherTextBase64);

            Cipher cipher =
                    Cipher.getInstance("AES/GCM/NoPadding");

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    keySpec,
                    new GCMParameterSpec(128, iv)
            );

            byte[] decrypted =
                    cipher.doFinal(cipherBytes);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    private record EncryptionResult(String cipherText, String iv) {}
}
