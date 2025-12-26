package com.shipmate.repository.auth;

import com.shipmate.model.auth.VerificationToken;
import com.shipmate.model.auth.VerificationTokenType;
import com.shipmate.model.user.User;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByUserAndTypeAndUsedFalse( User user, VerificationTokenType type );

    void deleteAllByUser(User user);
}
