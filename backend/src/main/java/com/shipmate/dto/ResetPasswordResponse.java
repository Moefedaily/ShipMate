package com.shipmate.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ResetPasswordResponse {
    private String message;
}
