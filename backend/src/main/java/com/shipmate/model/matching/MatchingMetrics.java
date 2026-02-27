package com.shipmate.model.matching;


public record MatchingMetrics(
        double distanceToPickupKm,
        double pickupToDeliveryKm,
        Double estimatedDetourKm,
        int score
) {}
