package com.shipmate.dto.response.admin;

import com.shipmate.model.user.Role;
import com.shipmate.model.user.UserType;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminUserResponse {

    private UUID id;

    private String email;

    private String firstName;

    private String lastName;

    private String phone;

    private Role role;

    private UserType userType;

    private boolean verified;

    private boolean active;

    private Instant createdAt;
}