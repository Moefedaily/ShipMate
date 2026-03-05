package com.shipmate.dto.response.admin;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserSummary {

    private UUID id;

    private String firstName;

    private String lastName;

    private String email;
}