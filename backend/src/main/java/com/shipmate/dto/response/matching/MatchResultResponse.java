package com.shipmate.dto.response.matching;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.shipmate.dto.response.shipment.ShipmentResponse;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResultResponse {

    private ShipmentResponse shipment;

    private MatchingMetricsResponse metrics;
}
