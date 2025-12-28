package com.shipmate.service.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.shipmate.model.auth.VerificationToken;
import com.shipmate.model.auth.VerificationTokenType;
import com.shipmate.model.user.User;
import com.shipmate.repository.auth.VerificationTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository repository;

    public VerificationToken createToken(
            User user,
            VerificationTokenType type,
            Duration ttl
    ) {
        repository.findByUserAndTypeAndUsedFalse(user, type)
                .ifPresent(existing -> {
                    existing.setUsed(true);
                    repository.save(existing);
                });

        VerificationToken token = VerificationToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .type(type)
                .expiresAt(Instant.now().plus(ttl))
                .used(false)
                .build();

        return repository.save(token);
    }

    public VerificationToken validateToken(String tokenValue, VerificationTokenType expectedType) {
        VerificationToken token = repository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (token.isUsed()) {
            throw new IllegalStateException("Token already used");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Token expired");
        }
        if (token.getType() != expectedType) throw new IllegalStateException("Invalid token type");

        return token;
    }

    public void markAsUsed(VerificationToken token) {
        token.setUsed(true);
        repository.save(token);
    }
}
