package com.shipmate.security;

import com.shipmate.model.user.UserType;
import com.shipmate.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
@Component("driverSecurity")
@RequiredArgsConstructor
public class DriverSecurity {

    private final UserRepository userRepository;

    public boolean isDriver(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());

        return userRepository.findById(userId)
                .map(user -> user.getUserType() == UserType.DRIVER)
                .orElse(false);
    }
}
