package com.shipmate.dto.response.auth;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VerifyEmailResponse {
    private String message;
}
