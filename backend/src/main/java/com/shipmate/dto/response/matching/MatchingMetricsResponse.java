package com.shipmate.dto.response.matching;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingMetricsResponse {

    private Double distanceToPickupKm;

    private Double pickupToDeliveryKm;

    private Double estimatedDetourKm;

    private Integer score;
}