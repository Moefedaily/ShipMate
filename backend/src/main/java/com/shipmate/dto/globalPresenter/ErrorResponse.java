package com.shipmate.dto.globalPresenter;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ErrorResponse {

    private final String message;
    private final int status;
    private final Instant timestamp;
    private String code;
}
