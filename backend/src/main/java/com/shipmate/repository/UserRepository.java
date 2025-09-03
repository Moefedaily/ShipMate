package com.shipmate.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shipmate.model.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
