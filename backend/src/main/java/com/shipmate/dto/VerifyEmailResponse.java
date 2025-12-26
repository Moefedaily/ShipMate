package com.shipmate.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VerifyEmailResponse {
    private String message;
}
