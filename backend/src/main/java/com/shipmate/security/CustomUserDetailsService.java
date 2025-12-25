package com.shipmate.security;

import com.shipmate.model.user.User;
import com.shipmate.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {

        User user;

        // LOGIN: identifier is email
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("User not found with email: " + identifier));
        }
        // JWT: identifier is UUID
        else {
            UUID userId;
            try {
                userId = UUID.fromString(identifier);
            } catch (IllegalArgumentException e) {
                throw new UsernameNotFoundException("Invalid user identifier");
            }

            user = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("User not found with id: " + identifier));
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getId().toString())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .disabled(!user.isVerified())
                .build();
    }
}
