package com.shipmate.dto.request.auth;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ResetPasswordResponse {
    private String message;
}
