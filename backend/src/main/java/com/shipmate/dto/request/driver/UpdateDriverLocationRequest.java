package com.shipmate.dto.request.driver;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDriverLocationRequest {

    private BigDecimal latitude;

    private BigDecimal longitude;
}
