package com.shipmate.dto.response.user;

import java.util.UUID;

import com.shipmate.model.user.Role;
import com.shipmate.model.user.UserType; 

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Role role;
    private UserType userType;
    private boolean verified;
    private boolean active;
    private String avatarUrl;
}